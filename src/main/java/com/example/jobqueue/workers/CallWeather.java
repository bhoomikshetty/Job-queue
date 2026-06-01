package com.example.jobqueue.workers;

import org.springframework.stereotype.Component;
import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;

import java.time.OffsetDateTime;

@Component
public class CallWeather implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Weather Job with ID: " + job.getId());
        // Add logic to call weather API based on job arguments
        double result = Math.random();
        if(result < 0.8) {
            System.out.println("Weather information retrieved successfully for Job ID: " + job.getId());
            return new JobHandlerResponse(true, "Weather information retrieved successfully.", OffsetDateTime.now());
        } else {
            System.out.println("Failed to retrieve weather information for Job ID: " + job.getId());
            return new JobHandlerResponse(false, "Failed to retrieve weather information.", OffsetDateTime.now());
        }
    }

    @Override
    public JobType getSupportedType() {
        return JobType.call_weather;
    }

}
