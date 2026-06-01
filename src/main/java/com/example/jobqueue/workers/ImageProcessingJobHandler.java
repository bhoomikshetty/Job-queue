package com.example.jobqueue.workers;

import org.springframework.stereotype.Component;
import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;

import java.time.OffsetDateTime;

@Component
public class ImageProcessingJobHandler implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Image Processing Job with ID: " + job.getId());
        // Add logic for image processing based on job arguments
        double result = Math.random();
        if(result < 0.8) {
            System.out.println("Image processed successfully for Job ID: " + job.getId());
            return new JobHandlerResponse(true, "Image processed successfully.", OffsetDateTime.now());
        } else {
            System.out.println("Failed to process image for Job ID: " + job.getId());
            return new JobHandlerResponse(false, "Failed to process image.", OffsetDateTime.now());
        }
    }

    @Override
    public JobType getSupportedType() {
        return JobType.image_processing;
    }

}