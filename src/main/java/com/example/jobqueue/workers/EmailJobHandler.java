package com.example.jobqueue.workers;

import org.springframework.stereotype.Component;
import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;

import java.time.OffsetDateTime;

@Component
public class EmailJobHandler implements JobHandler {

    @Override
    public JobHandlerResponse execute(Job job) {
        System.out.println("Executing Email Job with ID: " + job.toString());

        double result = Math.random();
        if(result < 0.8) {
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
