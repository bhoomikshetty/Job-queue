package com.bhoomik.jobqueue.worker;

import com.bhoomik.jobqueue.domain.JobType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {

    private final Map<JobType, JobHandler> jobHandlers;

    public JobHandlerRegistry(List<JobHandler> jobHandlersList) {
        this.jobHandlers = jobHandlersList.stream()
                .collect(Collectors.toMap(JobHandler::getSupportedType, h -> h));
    }

    public JobHandler getHandlerForJobType(JobType jobType) {
        JobHandler handler = this.jobHandlers.get(jobType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for job type: " + jobType);
        }
        return handler;
    }
}
