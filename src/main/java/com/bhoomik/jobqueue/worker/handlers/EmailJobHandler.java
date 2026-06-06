package com.bhoomik.jobqueue.worker.handlers;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobType;
import com.bhoomik.jobqueue.worker.JobHandler;
import com.bhoomik.jobqueue.worker.JobHandlerResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.lang.Thread;

@Component
public class EmailJobHandler implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Email Job with ID: " + job.getId());
        
        try {
            Thread.sleep(500 + (int)(Math.random() * 300));
        } 
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double result = Math.random();
        if (result < 0.8) {
            System.out.println("Email sent successfully for Job ID: " + job.getId());
            return new JobHandlerResponse(true, "Email sent successfully.", OffsetDateTime.now());
        } else {
            System.out.println("Failed to send email for Job ID: " + job.getId());
            return new JobHandlerResponse(false, "Failed to send email.", OffsetDateTime.now());
        }
    }

    @Override
    public JobType getSupportedType() {
        return JobType.send_email;
    }
}
