package com.bhoomik.jobqueue.worker;

import java.time.OffsetDateTime;

public class JobHandlerResponse {

    private final boolean success;
    private final String message;
    private final OffsetDateTime resultedAtInMs;

    public JobHandlerResponse(boolean success, String message, OffsetDateTime resultedAtInMs) {
        this.success = success;
        this.message = message;
        this.resultedAtInMs = resultedAtInMs;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public OffsetDateTime getResultedAtInMs() { return resultedAtInMs; }
}
