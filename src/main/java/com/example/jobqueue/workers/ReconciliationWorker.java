package com.example.jobqueue.workers;

import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.jobqueue.JobRepository;
import com.example.jobqueue.RedisService;

import java.util.List;
import com.example.jobqueue.Job;

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

}
