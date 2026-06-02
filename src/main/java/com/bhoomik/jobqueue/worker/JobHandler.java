package com.bhoomik.jobqueue.worker;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobType;

public interface JobHandler {
    JobHandlerResponse execute(Job job);
    JobType getSupportedType();
}
