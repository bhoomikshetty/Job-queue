package com.example.jobqueue.workers;

import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.jobqueue.JobRepository;
import com.example.jobqueue.RedisService;

import io.lettuce.core.BitFieldArgs.Offset;

import java.util.List;
import com.example.jobqueue.Job;
import java.util.Optional;

@Component
public class ReconciliationWorker {
    

    final JobRepository jobRepository;
    final RedisService redisService;

    ReconciliationWorker(JobRepository jobRepository, RedisService redisService) {
        this.jobRepository = jobRepository;
        this.redisService = redisService;
    }


    @Scheduled(cron = "0 */3 * * * *") // Runs every 3 minutes
    public void reconcile() {
        System.out.println("Reconciliation Worker is running...");
        
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

    // @Scheduled(cron = "0 */30 * * * *") // Runs every 30 minutes
    @Scheduled(cron = "*/25 * * * * *") // Runs every 25 seconds
    public void removeStuckProcessingJobs() {

        System.out.println("Reconciliation Worker - Removing stuck processing jobs...");
        // OffsetDateTime offset = OffsetDateTime.now().minusMinutes(30);
        OffsetDateTime offset = OffsetDateTime.now().minusSeconds(25);
        int stuckJobsCount = jobRepository.resetStuckProcessingJobs(offset);
        
        System.out.println("Reconciliation Worker - Removed " + stuckJobsCount + " stuck processing jobs.");
    }
}
