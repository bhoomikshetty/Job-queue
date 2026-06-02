package com.bhoomik.jobqueue.worker.handlers;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobType;
import com.bhoomik.jobqueue.worker.JobHandler;
import com.bhoomik.jobqueue.worker.JobHandlerResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class CallWeatherHandler implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Weather Job with ID: " + job.getId());
        double result = Math.random();
        if (result < 0.8) {
            System.out.println("Weather retrieved successfully for Job ID: " + job.getId());
            return new JobHandlerResponse(true, "Weather information retrieved successfully.", OffsetDateTime.now());
        } else {
            System.out.println("Failed to retrieve weather for Job ID: " + job.getId());
            return new JobHandlerResponse(false, "Failed to retrieve weather information.", OffsetDateTime.now());
        }
    }

    @Override
    public JobType getSupportedType() {
        return JobType.call_weather;
    }
}
