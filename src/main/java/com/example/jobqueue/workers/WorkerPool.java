package com.example.jobqueue.workers;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;


import com.example.jobqueue.JobRepository;
import com.example.jobqueue.RedisService;
import java.util.List;

import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;
import com.example.jobqueue.JobStatus;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ConcurrentHashMap;
import java.time.OffsetDateTime;

@Component
public class WorkerPool {
    
    final RedisService redisService;
    final JobHandlerRegistry jobHandlerRegistry;
    final JobRepository jobRepository;
    final ProcessJobService processJobService;

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        2,  // Core pool size
        4,  // Maximum pool size
        1,  // Keep alive time (seconds)
        TimeUnit.SECONDS,  // Time unit for keep alive time
        new LinkedBlockingQueue<>(),  // Task queue
        r -> {
            Thread t = new Thread(r);
            t.setName("WorkerPool-Thread-" + t.getId());
            t.setUncaughtExceptionHandler((thread, execption) -> {
                System.out.println("Uncaught exception in thread " + thread.getName() + ": " + execption.getMessage());
            });
            return t;
        }
    );

    public WorkerPool(RedisService redisService, JobHandlerRegistry jobHandlerRegistry, JobRepository jobRepository, ProcessJobService processJobService) {
        this.redisService = redisService;
        this.jobHandlerRegistry = jobHandlerRegistry;
        this.jobRepository = jobRepository;
        this.processJobService = processJobService;
    }

    @Scheduled(cron = "*/5 * * * * *") // Runs every 5 seconds
    public void printWorkerPoolStatus() {
        System.out.println("printWorkerPoolStatus  ");
        processJobService.verifyProcessedCount();
    }

    @Scheduled(cron = "*/10 * * * * *") // Runs every 10 seconds
    public void pollRedisForJobs() {

        List<String> jobs = redisService.getJobsFromRedis();
        System.out.println("Worker Pool logs: " + jobs);

        jobs.stream().forEach(jobId -> redisService.removeJobFromRedis(Long.parseLong(jobId)));
        jobs.stream().forEach(jobId -> {
            try{
                executor.execute(() -> {
                    processJobService.processJob(jobId);
                });
            }
            catch(Exception e) {
                System.out.println("Error processing job with ID " + jobId + ": " + e.getMessage());
            }
        });

    }


    // This is wrong Spring boot can't call this (because this is a self call, and the transaction management won't work on self calls). We need to move this to a separate service and call that service from here.
    // @Transactional
    // public void processJob(String jobId) {
    //     Optional<Job> job = jobRepository.findByIdAndLock(Long.parseLong(jobId));
    //     if (job.isPresent()) {
    //         JobHandler handler = jobHandlerRegistry.getHandlerForJobType(job.get().getType());
    //         handler.execute(job.get());
    //         jobRepository.updateJobStatus(Long.parseLong(jobId));
    //     } else {
    //         System.out.println("Job with ID " + jobId + " not found in database.");
    //     }
    // }
}

@Service
class ProcessJobService {

    final JobRepository jobRepository;
    final JobHandlerRegistry jobHandlerRegistry;

    Map<Long, Integer> processedCount = new ConcurrentHashMap<>();

    public ProcessJobService(JobRepository jobRepository, JobHandlerRegistry jobHandlerRegistry) {
        this.jobRepository = jobRepository;
        this.jobHandlerRegistry = jobHandlerRegistry;
    }

    public void verifyProcessedCount() {
        if(processedCount.isEmpty()) {
            // System.out.println("No jobs have been processed yet.");
            return;
        }

        int duplicateProcessing = 0;
        for (Map.Entry<Long, Integer> entry : processedCount.entrySet()) {
            if(entry.getValue() > 1) {
                System.out.println("Job with ID " + entry.getKey() + " has been processed " + entry.getValue() + " times.");
                duplicateProcessing++;
            }
        }
        if(duplicateProcessing == 0) {
            System.out.println("No duplicate processing detected. All jobs have been processed once.");
        }


    }

    @Transactional
    public void processJob(String jobId) {
        Optional<Job> job = jobRepository.findByIdAndLock(Long.parseLong(jobId), OffsetDateTime.now());
        
        if (job.isPresent()) {
            processedCount.put(Long.parseLong(jobId), processedCount.getOrDefault(Long.parseLong(jobId), 0) + 1);
            JobHandler handler = jobHandlerRegistry.getHandlerForJobType(job.get().getType());
            JobHandlerResponse response = handler.execute(job.get());
            boolean isSuccessful = response.isSuccess();
            String message = response.getMessage();
            OffsetDateTime resultedAtInMs = response.getResultedAtInMs();

            if (isSuccessful) {
                System.out.println("Job with ID " + jobId + " completed successfully.");
                try{
                   jobRepository.updateJobStatus(Long.parseLong(jobId), JobStatus.completed.name());
                }catch(Exception e) {
                    System.out.println("Error updating job status for Job ID " + jobId + ": " + e.getMessage());
                }
            }
            else {
                System.out.println("Job with ID " + jobId + " failed during execution.");
                
                Job updateJob = job.get();
                int retries = job.get().getRetries();
                int maxRetries = job.get().getMaxRetries();
                
                updateJob.setLastRetriedAtInMs(resultedAtInMs);
                updateJob.setErrorMessage(message);
                updateJob.setRetries(retries + 1);
                updateJob.setLastErrorAtInMs(resultedAtInMs);
                
                if(retries + 1 < maxRetries) {
                    processedCount.put(Long.parseLong(jobId), 0);
                    long backoffSeconds = Math.min((long) Math.pow(2, retries), 3600);
                    OffsetDateTime nextRetryAt = resultedAtInMs.plusSeconds(backoffSeconds); // Schedule next retry after 1 minute
                    
                    updateJob.setStatus(JobStatus.pending);
                    updateJob.setNextRetryAtInMs(nextRetryAt);
                }
                else {
                    System.out.println("Job with ID " + jobId + " has reached max retries. Marking as failed.");
                    updateJob.setStatus(JobStatus.dead);
                }

                jobRepository.updateJob(
                    updateJob.getId(),
                    updateJob.getRetries(),
                    updateJob.getLastRetriedAtInMs(),
                    updateJob.getErrorMessage(),
                    updateJob.getLastErrorAtInMs(),
                    updateJob.getNextRetryAtInMs(),
                    updateJob.getStatus().name()
                );
            }
        } else {
            System.out.println("Job with ID " + jobId + " is not pending.");
        }
    }   
}   