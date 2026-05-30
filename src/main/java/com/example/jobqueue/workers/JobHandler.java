package com.example.jobqueue.workers;


import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;

public interface JobHandler {
    Boolean execute(Job job);
    JobType getSupportedType();
}

