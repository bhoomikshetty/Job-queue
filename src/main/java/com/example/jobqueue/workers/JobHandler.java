package com.example.jobqueue.workers;


import com.example.jobqueue.Job;
import com.example.jobqueue.JobType;
import java.time.OffsetDateTime;

public interface JobHandler {
    JobHandlerResponse execute(Job job);
    JobType getSupportedType();
}

class JobHandlerResponse {
    private boolean success;
    private String message;
    private OffsetDateTime resultedAtInMs;

    public JobHandlerResponse(boolean success, String message, OffsetDateTime resultedAtInMs) {
        this.success = success;
        this.message = message;
        this.resultedAtInMs = resultedAtInMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getResultedAtInMs() {
        return resultedAtInMs;
    }
}

