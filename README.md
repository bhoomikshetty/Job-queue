# Distributed Job Queue

A production-grade background job processing system built with Spring Boot, Redis, and Postgres.

## Architecture

```
POST /jobs
    │
    ▼
Postgres (source of truth)
jobs table: id, type, payload, status, retries,
            maxRetries, nextRetryAt, lastError,
            lockedAt, scheduledAt, createdAt
    │
    ▼
Redis Sorted Set (scheduling index)
key:   job_queue
score: scheduledAt epoch ms      ← ZRANGEBYSCORE fetches all due jobs in O(log N)
value: jobId
    │
    ▼
Worker Pool (ThreadPoolExecutor — 10 core / 20 max threads)
    │
    ├── Poll Redis ZSet every 2s for due jobs
    ├── Fetch + lock from Postgres (FOR UPDATE SKIP LOCKED)
    ├── Route to handler via JobHandlerRegistry
    ├── Execute handler
    └── On success → COMPLETED
        On failure → retries++
                   → if retries < maxRetries: PENDING + exponential backoff
                   → if retries >= maxRetries: DEAD (DLQ)
    │
    ▼
Resilience
    ├── Reconciliation job (every 3 min) — syncs missed jobs back to Redis
    └── Lease timeout (every 30 min) — resets stuck PROCESSING jobs to PENDING

    │
    ▼
Observability
    ├── Micrometer metrics → /actuator/prometheus
    └── GET /jobs/stats — live count by status
```

## Key Design Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Concurrent workers | `FOR UPDATE SKIP LOCKED` | Workers skip locked rows instead of blocking — true parallelism |
| Scheduling index | Redis Sorted Set | Score = epoch ms, `ZRANGEBYSCORE` fetches all due jobs in O(log N) |
| Source of truth | Postgres | Redis is ephemeral — Postgres survives restarts and Redis data loss |
| Retry backoff | `2^retryCount` seconds, capped at 3600 | Exponential with ceiling prevents thundering herd |
| Dead letter queue | `status = dead` in same table | No JOIN needed, simpler ops, easy manual requeue |
| Worker crash recovery | Lease timeout on `locked_at` | Stuck PROCESSING jobs reset to PENDING automatically |
| Metrics | Micrometer + Prometheus | Backend-agnostic facade — swap to Datadog with zero app changes |

Full decision log with trade-offs and production failure scenarios: [Architecture Decisions](docs/architecture-decisions.md)

## Stack

- **Java 17** + **Spring Boot 3.4**
- **Postgres 15** — job persistence, `FOR UPDATE SKIP LOCKED` row locking
- **Redis 7** — Sorted Set scheduling index
- **Flyway** — schema migrations
- **Micrometer + Prometheus** — metrics
- **k6** — load testing

## Running Locally

**Run docker compose to spin up Postgres, Redis, and the app:**

```bash
docker compose up --build
```

App is available at `http://localhost:3000`.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/addJob` | Submit a new job |
| `GET` | `/getJobById?id=<id>` | Get job by ID — status, retries, error |
| `GET` | `/getAllJobs` | List all jobs |
| `GET` | `/getPendingJobs?status=<status>` | Filter jobs by status |
| `GET` | `/jobs/stats` | Live count by status |
| `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |

**Submit a job:**

```bash
curl -X POST http://localhost:3000/addJob \
  -H "Content-Type: application/json" \
  -d '{
    "job_name": "send-welcome-email",
    "job_type": "send_email",
    "max_retries": 3,
    "jobs_args": { "from": "noreply@example.com", "to": "user@example.com" }
  }'
```

**Job types:** `send_email`, `call_weather`, `image_processing`

**Job lifecycle:**

```
PENDING → PROCESSING → COMPLETED
                    ↘
                     FAILED → (retry with backoff) → PENDING
                            → (max retries hit)   → DEAD
```

## Observability

**Queue health snapshot:**

```bash
curl http://localhost:3000/jobs/stats
```

```json
{
  "pending": 12,
  "processing": 4,
  "completed": 4821,
  "failed": 0,
  "dead": 3
}
```

**Prometheus metrics** at `/actuator/prometheus`:

| Metric | Type | Description |
|--------|------|-------------|
| `jobqueue_jobs_processed_total` | Counter | Total completed jobs |
| `jobqueue_jobs_failed_total` | Counter | Total failed jobs |
| `jobqueue_jobs_processing_time_seconds` | Timer | p50/p95/p99 processing latency |
| `jobqueue_jobs_pending` | Gauge | Current pending job count |

## Load Test

Run with k6 (requires Docker):

```bash
docker run --rm -v "${PWD}:/scripts" grafana/k6 run /scripts/load-test.js
```

All numbers measured with realistic handler latencies: email 500–800ms, image 1000–1500ms, weather 100–300ms.

**Sustained load vs stress (k6, 30s duration):**

| Config | VUs | HTTP p95 | E2E median | E2E p95 | Notes |
|--------|-----|----------|------------|---------|-------|
| Sustainable | 20 | 304ms | 2.4s | 39s | 82% completion — p95 tail is retry backoff, not system latency |
| Stress | 75 | 342ms | 4.6s | 42s | 80% completion — worker pool saturates, HTTP layer stays healthy |

75 VUs was chosen deliberately: a separate breakpoint test (ramp to 300 VUs) showed the HTTP layer
breaks at ~200 VUs. Running at 75 sits between the sustainable point and the HTTP limit, which
reveals the worker pool as the bottleneck in isolation. At 75 VUs, HTTP p95 is only 342ms
with 0% request failures — the API is fine. But e2e median doubles (2.4s → 4.6s) because
jobs are arriving faster than the 10-core pool can drain them.

**Breakpoint test — ramp to 300 VUs:**

| Stage | VUs | Behaviour |
|-------|-----|-----------|
| Warm up | 20 | Stable — 0% failures |
| Comfortable | 50–100 | Stable — no degradation |
| Stress | 200–300 | HTTP layer breaks — 4% request failures, p95 crossed 1s |
