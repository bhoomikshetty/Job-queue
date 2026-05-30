package com.example.jobqueue.workers;

import org.springframework.stereotype.Component;
import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;

@Component
public class ImageProcessingJobHandler implements JobHandler {

    @Override
    public Boolean execute(Job job) {
        System.out.println("Executing Image Processing Job with ID: " + job.getId());
        // Add logic for image processing based on job arguments
        double result = Math.random();
        if(result < 0.8) {
            System.out.println("Email sent successfully for Job ID: " + job.getId());
            return true;
        } else {
            System.out.println("Failed to send email for Job ID: " + job.getId());
            return false;
        }
    }

    @Override
    public JobType getSupportedType() {
        return JobType.image_processing;
    }

}