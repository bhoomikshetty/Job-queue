# Architecture Decision Records

Architectural decisions made during the design and implementation of this distributed job queue.
Each entry documents what was chosen, why, what was rejected, and known failure modes.

---

## ADR 1: FOR UPDATE SKIP LOCKED for concurrent worker locking

**Decision:** Use a native Postgres query with `FOR UPDATE SKIP LOCKED` instead of `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

**Context:** Multiple workers run concurrently and must not process the same job twice.

**Rationale:**
`@Lock` blocks — if Worker A holds the lock, Workers B and C queue and wait. Under load this causes worker starvation: all threads pile up on one row instead of processing other available jobs. `FOR UPDATE SKIP LOCKED` tells Postgres to skip rows already locked and grab the next available one. Workers never block each other — true parallelism.

The same pattern is used by Sidekiq, GoodJob, and Oban in production.

**Failure modes:**
1. Worker crashes after locking but before updating status to `processing` — row stays locked until the transaction times out. Mitigated by the lease timeout task (ADR 4).
2. All jobs have a future `scheduled_at` — workers find nothing. Requires an index on `(status, scheduled_at)`.
3. Too many workers on a small table — high contention even with SKIP LOCKED. Fix: batch fetch with `LIMIT N` per poll cycle.

---

## ADR 2: Redis Sorted Set as scheduling index

**Decision:** Use a Redis Sorted Set (score = `scheduledAt` epoch ms, member = `jobId`) as the scheduling layer on top of Postgres.

**Context:** Workers need to efficiently find all jobs due for execution right now without hammering Postgres with repeated range scans under concurrent load.

**Rationale:**
`ZRANGEBYSCORE job_queue 0 {now_ms}` returns every due job in O(log N), in-memory, sub-millisecond. Polling Redis first to get due job IDs, then fetching only those from Postgres, separates the scheduling concern from the execution concern and reduces DB read pressure at scale.

**Key operations:**
```
ZADD job_queue {scheduledAt_ms} {jobId}   — enqueue
ZRANGEBYSCORE job_queue 0 {now_ms}        — fetch all due jobs
ZREM job_queue {jobId}                    — remove after pickup
```

**Failure modes:**
1. Redis goes down — workers can't find due jobs. Mitigated by the reconciliation job (ADR 8) which falls back to direct Postgres polling.
2. ZADD fails after Postgres INSERT — job exists in Postgres but never scheduled. Mitigated by reconciliation.
3. Clock skew between app instances causes incorrect `scheduledAt`. Fix: use DB `now()` for all time calculations, not application server time.

---

## ADR 3: Postgres as source of truth

**Decision:** Postgres holds canonical job state. Redis is a cache and scheduling index only.

**Rationale:**
Redis persistence (AOF/RDB) adds operational complexity and neither provides ACID guarantees, audit trail, or complex query capability. Postgres provides all of these. If Redis and Postgres ever disagree, Postgres wins — the ZSet is always reconstructable by scanning Postgres for `PENDING` jobs. The reverse is not true.

**Failure modes:**
1. Postgres goes down — entire system stops. Mitigation: replica for reads, primary for writes.
2. Redis and Postgres diverge — job in ZSet but `COMPLETED` in Postgres. Worker picks it up and attempts re-execution. Fix: always verify Postgres status after Redis fetch before executing.
3. Long-running Postgres transactions holding locks during high write volume. Fix: keep job state update transactions short — single UPDATE, no multi-step transactions.

---

## ADR 4: Exponential backoff with cap for retries

**Decision:** `nextRetryAt = now + min(2^retryCount, 3600)` seconds.

**Context:** Failed jobs must be retried without overwhelming a downstream service that is already struggling.

**Rationale:**
Fixed retry intervals hammer a failing downstream service at a constant rate. Exponential backoff gives the downstream service time to recover between attempts. The cap at 3600 seconds (1 hour) prevents unbounded delay growth.

Jitter (`+ random(0, base)`) prevents thundering herd — without it, all jobs that failed at the same time retry at the same time on recovery.

**Failure modes:**
1. `retryCount` not capped — overflow possible on very long-running jobs. Fix: always check `retryCount < maxRetries` before incrementing.
2. `nextRetryAt` calculated using app server time instead of DB time — clock skew between instances causes premature or delayed retries.
3. Permanent failures (invalid payload, bad job type) retried until `maxRetries`. Fix: classify errors as transient vs permanent — permanent errors skip directly to `dead`.

---

## ADR 5: Dead letter queue as `status = dead` in the same table

**Decision:** Jobs exceeding `maxRetries` are marked `status = dead` in the jobs table. No separate DLQ table.

**Rationale:**
A separate table adds JOIN complexity for dashboards and monitoring. `dead` as a status keeps all job data in one place — full history, retry count, last error, all visible in one query: `SELECT * FROM jobs WHERE status = 'dead'`.

**Failure modes:**
1. `dead` jobs accumulate unboundedly — table grows without bound. Fix: archival job that moves old dead jobs to cold storage after N days.
2. Requeueing a permanently-failing job — it fails again and returns to `dead`. Fix: require payload inspection or manual fix before requeueing.
3. No alerting on dead job spike — silent accumulation. Fix: Prometheus alert on rate of increase of `jobqueue_jobs_failed_total`.

---

## ADR 6: Redis ZSet over Kafka for job scheduling

**Decision:** Redis Sorted Set for scheduling, not Kafka consumer groups as workers.

**Context:** Considered Kafka given existing expertise with Strimzi at 3.6M events/sec.

**Rationale:**
Kafka is a log — consumers read in arrival order. Delayed/scheduled execution requires either sleeping in the consumer (blocks the thread), building a delay-topic polling loop (complex workaround), or adding a separate scheduler layer (rebuilds what ZSet provides natively).

Redis ZSet score IS the execution timestamp. Scheduling a job 30 minutes from now is a single ZADD with `score = now + 1800000ms`. No workarounds needed.

| Capability | Kafka | Redis ZSet |
|-----------|-------|------------|
| High throughput | Excellent | Good |
| Consumer group scaling | Native | Manual |
| Replay / rewind | Native | No |
| Delayed / scheduled jobs | Workaround | Native |
| Exactly-once processing | With config | Harder |
| Persistence / durability | Built-in | Needs AOF |
| Operational complexity | Higher | Lower |

Kafka is the right choice when: pure throughput with no scheduling requirement, replay/rewind of job history is needed, or consumer group auto-scaling across many instances is required.

The same ZSet pattern is used by PhonePe Clockwork (2B daily jobs), Sidekiq, and Oban.

**Failure modes:**
1. No consumer group concept — two worker instances can both fetch the same `jobId` from `ZRANGEBYSCORE` before either removes it. `FOR UPDATE SKIP LOCKED` in Postgres is the actual exclusion guard.
2. ZSet grows unboundedly if `ZREM` is missed on job pickup failure. Fix: cleanup task removes ZSet entries for jobs already `completed` or `dead` in Postgres.
3. High-cardinality ZSet under heavy write load — `ZREM` contention. Fix: pipeline `ZRANGEBYSCORE` + `ZREM` in a single Redis transaction.

---

## ADR 7: `locked_by_worker_id` as VARCHAR, not a foreign key

**Decision:** `locked_by_worker_id VARCHAR(255)` — a plain string identifier on the jobs row, not a foreign key to a workers table.

**Rationale:**
`lockedBy` exists for one purpose: identifying which worker held a job when it got stuck. A string like `worker1@hostname` or `pod-uuid:thread-id` is sufficient for crash detection and debugging. A full workers table would require worker registration before job pickup, add FK ordering constraints, and introduce lifecycle complexity for ephemeral Kubernetes pods.

Sidekiq, Oban, and GoodJob all use a plain string for `lockedBy`.

**Failure modes:**
1. Two workers generate the same `lockedBy` string — can't distinguish which crashed. Fix: include PID or UUID in the worker identifier.
2. `lockedBy` stays stale — worker is long gone but string remains. Lease timeout cleanup resets it when resetting status to `pending`.
3. No index on `lockedBy` — slow to query all jobs held by a specific worker. Acceptable trade-off since this query only runs during crash recovery, not on the hot path.

---

## ADR 8: Reconciliation job for dual-write failure

**Context:** When a job is created, two writes happen: INSERT to Postgres, then ZADD to Redis. The ZADD can fail — the job exists in Postgres but workers never see it.

**What was rejected:** A `redis_synced` boolean field. If Redis crashes and loses all in-memory data, `redis_synced` is already `true` on existing rows — reconciliation would skip them and jobs would be stuck indefinitely.

**Decision:** No `redis_synced` field. Reconciliation queries `PENDING` jobs directly from Postgres.

On job creation: save to Postgres, attempt ZADD, if ZADD fails do nothing — reconciliation handles it.

Reconciliation job (runs every 3 minutes):
```sql
WHERE status = 'pending' AND scheduled_at <= now() + interval '5 minutes'
```
ZADD is idempotent — re-adding an existing member just updates the score. No deduplication needed.

**Why this handles every failure scenario:**
- Initial ZADD failure → reconciliation re-adds within 3 minutes
- Redis crash + data loss → reconciliation rebuilds ZSet from all PENDING jobs on next cycle
- Two workers fetching the same job → `FOR UPDATE SKIP LOCKED` is the actual exclusion guard

**Failure modes:**
1. Reconciliation job crashes repeatedly — PENDING jobs accumulate. Fix: alert on `status = pending AND created_at < now - 10 minutes AND scheduled_at <= now`.
2. Burst of many jobs all scheduled simultaneously — all fall within the lookahead window at once. Fix: paginate results and pipeline ZADDs per batch.
3. Lookahead window too narrow — job scheduled 6 minutes from now isn't pushed to Redis until 3 minutes before due. Tune window based on observed worker poll frequency.

---

## ADR 9: `@Transactional` on `@Modifying` queries

**Decision:** `@Transactional` only where rollback capability is needed. Standalone `@Modifying` queries rely on Spring Boot's auto-commit behavior.

**Distinction:**
- `@Modifying` — contract with Spring Data JPA: execute as DML, not a SELECT. Without it, JPA tries to map the result as a query result set.
- `@Transactional` — contract with the database: wrap in a transaction that can be committed or rolled back. Required when multiple writes must succeed or fail together.

Single updates like `updateJobStatus` are uncontested writes — no prior write to roll back if they fail. Adding `@Transactional` adds overhead with no safety benefit. Multi-field updates like `updateJob` (retries + lastRetriedAt + status) need `@Transactional` so a partial failure doesn't leave the row in an inconsistent state.

**Failure modes:**
1. Datasource configured with `autoCommit=false` — standalone `@Modifying` queries silently do nothing. Fix: verify datasource auto-commit settings in production config.
2. Partial write without rollback — `incrementRetries` succeeds but `updateJobStatus` fails. Retry count is incremented but status stays `processing`. Fix: wrap related writes in one `@Transactional`.
3. Failed UPDATE affects 0 rows silently. Always verify affected row count on critical updates.

---

## ADR 10: ConcurrentHashMap with `merge()` for thread-safe counters

**Decision:** `ConcurrentHashMap` with atomic `merge()` for tracking per-job processing counts across worker threads.

**Rationale:**
`HashMap` is not thread-safe — concurrent writes can corrupt internal structure or silently drop increments. `ConcurrentHashMap` uses bucket-level locking for safe concurrent access.

`getOrDefault() + put()` is a non-atomic read-modify-write — two threads can race and lose an increment. `merge()` is a single atomic operation.

```java
// non-atomic — increment can be lost under concurrency
map.put(jobId, map.getOrDefault(jobId, 0) + 1);

// atomic
map.merge(jobId, 1, Integer::sum);
```

**Failure modes:**
1. `HashMap` used assuming single-threaded access — a future refactor adding a second thread introduces a silent race condition.
2. In-memory state resets on restart — not suitable for durable idempotency. Fix: persist to Redis or Postgres if durability is required.
3. Unbounded map growth as job IDs accumulate. Fix: evict entries after a TTL or use a bounded cache.

---

## ADR 11: `execute()` with try-catch in lambda for worker thread exception logging

**Decision:** Use `executor.execute()` with a try-catch inside the lambda rather than relying on `UncaughtExceptionHandler` alone.

**Rationale:**
`submit()` wraps tasks in a `Future` and catches all exceptions internally — they are stored in the Future and never propagate to `UncaughtExceptionHandler`. The handler fires silently. `execute()` propagates exceptions to the handler, but loses task context — the jobId that caused the failure is not available.

Wrapping `processJob(jobId)` in a try-catch inside the lambda captures `jobId` via closure — every crash is logged with the specific job that caused it.

**Failure modes:**
1. try-catch catches `Exception` but not `Error` — `OutOfMemoryError` and `StackOverflowError` won't be caught. Fix: catch `Throwable` on critical paths.
2. Async log aggregation may lose logs if JVM crashes immediately after. Fix: synchronous logging for critical error paths.
3. Unbounded `LinkedBlockingQueue` — under extreme load the queue grows consuming heap. Fix: bounded queue with a `RejectedExecutionHandler`.

---

## ADR 12: Micrometer with tagged counters for metrics

**Decision:** Micrometer as the metrics facade with Prometheus as the backend. Counters tagged by `attempt` (`first` vs `retry`) to separate first-attempt performance from retry performance.

**Rationale:**
Rolling custom metrics requires implementing the Prometheus text format, thread-safe counters, and histogram bucketing manually. Micrometer handles all of this and is backend-agnostic — swapping to Datadog or CloudWatch requires zero application code changes.

Tagging by attempt type separates two meaningfully different distributions: first-attempt jobs complete in ~2 seconds median; retried jobs have higher latency because they sit in exponential backoff queues. Mixing them produces a misleading p95.

**Metrics:**
| Metric | Type | Purpose |
|--------|------|---------|
| `jobqueue.jobs.processed{attempt}` | Counter | Throughput by attempt type |
| `jobqueue.jobs.failed{attempt}` | Counter | Error rate by attempt type |
| `jobqueue.jobs.processing.time` | Timer | p50/p95/p99 processing latency |
| `jobqueue.jobs.pending` | Gauge | Live queue depth |

Prometheus scrapes `/actuator/prometheus` on a pull model every 15 seconds. The app never initiates transfer.

**Failure modes:**
1. Gauge queries Postgres on every scrape — at high scrape frequency this adds read load. Fix: cache the gauge value with a short TTL using a scheduled task.
2. High-cardinality tags (e.g. tagging by job ID) — one time series per job, Prometheus OOM. Fix: only tag by low-cardinality dimensions like job type or attempt.
3. Default Timer histogram buckets may not align with your SLA targets. Fix: configure custom SLO buckets at registration time.
