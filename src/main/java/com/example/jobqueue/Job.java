package com.example.jobqueue;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jobs", schema = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "name")
    @JsonProperty("job_name")
    String name;

    
    @Column(name = "status")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING)
    JobStatus status = JobStatus.pending;
    
    @Column(name = "type")
    @JsonProperty("job_type")
    @Enumerated(EnumType.STRING)
    JobType type;
    
    @Column(name = "args_json_string")
    @JsonProperty("jobs_args")
    @JdbcTypeCode(SqlTypes.JSON)
    Map<String, Object> args;
    
    // Details about the worker locks. q
    @Column(name = "locked_by_worker_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String lockedByWorkerId;
    
    @Column(name = "locked_at")
    @JsonProperty(namespace = "locked_at_in_ms", access = JsonProperty.Access.READ_ONLY)    
    OffsetDateTime lockedAtInMs;

    //TODO: also think about the logging part
    @Column(name = "created_at")
    @JsonProperty(namespace = "job_created_at_in_ms", access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime createdAtInMs = OffsetDateTime.now();

    @Column(name = "next_retry_at")
    @JsonProperty(namespace = "next_retry_at_in_ms", access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime nextRetryAtInMs;

    @Column(name = "scheduled_at")
    @JsonProperty(namespace = "scheduled_at_in_ms")
    OffsetDateTime scheduledAtInMs = OffsetDateTime.now();

    @Column(name = "max_retries")
    @JsonProperty("max_retries")
    int maxRetries = 0;
    
    @Column(name = "retries")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    int retries = 0;
    
    @Column(name = "last_retried_at")
    @JsonProperty(namespace = "last_retried_at_in_ms", access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime lastRetriedAtInMs;

    @Column(name = "error_message")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String errorMessage;
    

    @Column(name = "last_error_at")
    @JsonProperty(namespace = "last_error_at_in_ms", access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime lastErrorAtInMs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public String getArgs() {
        return args.toString();
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAtInMs() {
        return createdAtInMs;
    }

    public void setCreatedAtInMs(OffsetDateTime createdAtInMs) {
        this.createdAtInMs = createdAtInMs;
    }

    public OffsetDateTime getLastRetriedAtInMs() {
        return lastRetriedAtInMs;
    }

    public void setLastRetriedAtInMs(OffsetDateTime lastRetriedAtInMs) {
        this.lastRetriedAtInMs = lastRetriedAtInMs;
    }

    public Job(Job other) {
        this.name = other.name;
        this.maxRetries = other.maxRetries;
        this.retries = other.retries;
        this.status = other.status;
        this.type = other.type;
        this.args = other.args;
        this.errorMessage = other.errorMessage;
        this.createdAtInMs = other.createdAtInMs;
        this.lastRetriedAtInMs = other.lastRetriedAtInMs;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxRetries=" + maxRetries +
                ", retries=" + retries +
                ", status=" + status +
                ", type=" + type +
                ", args='" + args + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAtInMs='" + createdAtInMs + '\'' +
                ", lastRetriedAtInMs='" + lastRetriedAtInMs + '\'' +
                '}';



    }

    public String getLockedByWorkerId() {
        return lockedByWorkerId;
    }

    public void setLockedByWorkerId(String lockedByWorkerId) {
        this.lockedByWorkerId = lockedByWorkerId;
    }

    public OffsetDateTime getLockedAtInMs() {
        return lockedAtInMs;
    }

    public void setLockedAtInMs(OffsetDateTime lockedAtInMs) {
        this.lockedAtInMs = lockedAtInMs;
    }

    public OffsetDateTime getLastErrorAtInMs() {
        return lastErrorAtInMs;
    }

    public void setLastErrorAtInMs(OffsetDateTime lastErrorAtInMs) {
        this.lastErrorAtInMs = lastErrorAtInMs;
    }

    public OffsetDateTime getNextRetryAtInMs() {
        return nextRetryAtInMs;
    }

    public void setNextRetryAtInMs(OffsetDateTime nextRetryAtInMs) {
        this.nextRetryAtInMs = nextRetryAtInMs;
    }

    public OffsetDateTime getScheduledAtInMs() {
        return scheduledAtInMs;
    }

    public void setScheduledAtInMs(OffsetDateTime scheduledAtInMs) {
        this.scheduledAtInMs = scheduledAtInMs;
    }
}




