package com.bhoomik.jobqueue.metrics;

import com.bhoomik.jobqueue.domain.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class JobMetricsService {

    private final MeterRegistry registry;
    private final Timer jobProcessingTime;

    public JobMetricsService(MeterRegistry registry, JobRepository jobRepository) {
        this.registry = registry;

        this.jobProcessingTime = Timer.builder("jobqueue.jobs.processing.time")
            .description("Time taken to process a job")
            .register(registry);

        Gauge.builder("jobqueue.jobs.pending", jobRepository, repo -> repo.countPendingJobs())
            .description("Number of pending jobs")
            .register(registry);
    }

    public void recordJobProcessed(int retries) {
        String attempt = retries == 0 ? "first" : "retry";
        Counter.builder("jobqueue.jobs.processed")
            .description("Total jobs completed")
            .tag("attempt", attempt)
            .register(registry)
            .increment();
    }

    public void recordJobFailed(int retries) {
        String attempt = retries == 0 ? "first" : "retry";
        Counter.builder("jobqueue.jobs.failed")
            .description("Total jobs failed")
            .tag("attempt", attempt)
            .register(registry)
            .increment();
    }

    public void recordProcessingTime(Duration duration) {
        jobProcessingTime.record(duration);
    }
}
