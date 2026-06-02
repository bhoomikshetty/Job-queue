package com.bhoomik.jobqueue.resilience;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobRepository;
import com.bhoomik.jobqueue.scheduler.RedisService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ReconciliationWorker {

    final JobRepository jobRepository;
    final RedisService redisService;

    ReconciliationWorker(JobRepository jobRepository, RedisService redisService) {
        this.jobRepository = jobRepository;
        this.redisService = redisService;
    }

    // Catches jobs that were persisted to Postgres but missed Redis (dual-write failure fallback)
    @Scheduled(cron = "0 */3 * * * *")
    public void reconcile() {
        System.out.println("Reconciliation: syncing pending jobs to Redis...");
        OffsetDateTime offset = OffsetDateTime.now().plusMinutes(5);
        List<Job> pendingJobs = jobRepository.fetchScheduledJobs(offset);

        for (Job job : pendingJobs) {
            try {
                redisService.addJobToRedis(job);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Resets jobs stuck in 'processing' whose lease expired — handles worker crashes
    @Scheduled(cron = "0 */30 * * * *")
    public void removeStuckProcessingJobs() {
        System.out.println("Reconciliation: resetting stuck processing jobs...");
        OffsetDateTime offset = OffsetDateTime.now().minusMinutes(30);
        int stuckJobsCount = jobRepository.resetStuckProcessingJobs(offset);
        System.out.println("Reconciliation: reset " + stuckJobsCount + " stuck jobs to pending.");
    }
}
