package com.bhoomik.jobqueue.worker.handlers;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobType;
import com.bhoomik.jobqueue.worker.JobHandler;
import com.bhoomik.jobqueue.worker.JobHandlerResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ImageProcessingJobHandler implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Image Processing Job with ID: " + job.getId());
        double result = Math.random();
        if (result < 0.8) {
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
