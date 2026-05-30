package com.example.jobqueue.workers;

import org.springframework.stereotype.Component;

import com.example.jobqueue.JobType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;


@Component
public class JobHandlerRegistry {
    
    final private Map<JobType, JobHandler> jobHandlers;
    
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
