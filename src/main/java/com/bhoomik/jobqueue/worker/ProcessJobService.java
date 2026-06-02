package com.bhoomik.jobqueue.worker;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobRepository;
import com.bhoomik.jobqueue.domain.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProcessJobService {

    final JobRepository jobRepository;
    final JobHandlerRegistry jobHandlerRegistry;

    Map<Long, Integer> processedCount = new ConcurrentHashMap<>();

    public ProcessJobService(JobRepository jobRepository, JobHandlerRegistry jobHandlerRegistry) {
        this.jobRepository = jobRepository;
        this.jobHandlerRegistry = jobHandlerRegistry;
    }

    public void verifyProcessedCount() {
        if (processedCount.isEmpty()) return;

        int duplicateProcessing = 0;
        for (Map.Entry<Long, Integer> entry : processedCount.entrySet()) {
            if (entry.getValue() > 1) {
                System.out.println("Job ID " + entry.getKey() + " processed " + entry.getValue() + " times.");
                duplicateProcessing++;
            }
        }
        if (duplicateProcessing == 0) {
            System.out.println("No duplicate processing detected.");
        }
    }

    @Transactional
    public void processJob(String jobId) {
        Optional<Job> job = jobRepository.findByIdAndLock(Long.parseLong(jobId), OffsetDateTime.now());

        if (job.isPresent()) {
            processedCount.merge(Long.parseLong(jobId), 1, Integer::sum);

            JobHandler handler = jobHandlerRegistry.getHandlerForJobType(job.get().getType());
            JobHandlerResponse response = handler.execute(job.get());

            if (response.isSuccess()) {
                System.out.println("Job " + jobId + " completed successfully.");
                try {
                    jobRepository.updateJobStatus(Long.parseLong(jobId), JobStatus.completed.name());
                } catch (Exception e) {
                    System.out.println("Error updating status for Job " + jobId + ": " + e.getMessage());
                }
            } else {
                System.out.println("Job " + jobId + " failed.");

                Job updateJob = job.get();
                int retries = updateJob.getRetries();
                int maxRetries = updateJob.getMaxRetries();
                OffsetDateTime resultedAt = response.getResultedAtInMs();

                updateJob.setLastRetriedAtInMs(resultedAt);
                updateJob.setErrorMessage(response.getMessage());
                updateJob.setRetries(retries + 1);
                updateJob.setLastErrorAtInMs(resultedAt);

                if (retries + 1 < maxRetries) {
                    processedCount.put(Long.parseLong(jobId), 0);
                    long backoffSeconds = Math.min((long) Math.pow(2, retries), 3600);
                    updateJob.setNextRetryAtInMs(resultedAt.plusSeconds(backoffSeconds));
                    updateJob.setStatus(JobStatus.pending);
                } else {
                    System.out.println("Job " + jobId + " reached max retries. Marking as dead.");
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
            System.out.println("Job " + jobId + " is not pending (already picked up or completed).");
        }
    }
}
