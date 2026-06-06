package com.bhoomik.jobqueue.worker;

import com.bhoomik.jobqueue.scheduler.RedisService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class WorkerPool {

    final RedisService redisService;
    final ProcessJobService processJobService;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        10,
        20,
        1,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = new Thread(r);
            t.setName("WorkerPool-Thread-" + t.getId());
            t.setUncaughtExceptionHandler((thread, ex) ->
                System.out.println("Uncaught exception in thread " + thread.getName() + ": " + ex.getMessage()));
            return t;
        }
    );

    public WorkerPool(RedisService redisService, ProcessJobService processJobService) {
        this.redisService = redisService;
        this.processJobService = processJobService;
    }

    // @Scheduled(cron = "*/5 * * * * *")
    // public void printWorkerPoolStatus() {
    //     processJobService.verifyProcessedCount();
    // }
    

    @Scheduled(cron = "*/2 * * * * *")
    public void pollRedisForJobs() {
        List<String> jobs = redisService.getJobsFromRedis();
        System.out.println("Worker Poll: " + jobs);

        jobs.forEach(jobId -> redisService.removeJobFromRedis(Long.parseLong(jobId)));
        jobs.forEach(jobId -> {
            try {
                executor.execute(() -> processJobService.processJob(jobId));
            } catch (Exception e) {
                System.out.println("Error dispatching job " + jobId + ": " + e.getMessage());
            }
        });
    }
}
