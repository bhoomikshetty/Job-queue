package com.example.jobqueue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Map;

@Slf4j
@Entity
@Table(name = "jobs", schema = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "name")
    @JsonProperty("job_name")
    String name;

    @Column(name = "max_retries")
    @JsonProperty("max_retries")
    int maxRetries = 0;


    @Column(name = "retries")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    int retries = 0;

    @Column(name = "status")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated()
    JobStatus status = JobStatus.pending;

    @Column(name = "type")
    @JsonProperty("job_type")
    @Enumerated()
    JobType type;

    @Column(name = "args_json_string")
    @JsonProperty("jobs_args")
    String args;

    @Column(name = "error_message")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String errorMessage;

    //TODO: also think about the logging part
    @Column(name = "created_at")
    @JsonProperty("job_created_at_in_ms")
    String createdAtInMs;

    @Column(name = "last_retried_at")
    @JsonProperty(namespace = "last_retried_at_in_ms", access = JsonProperty.Access.READ_ONLY)
    String lastRetriedAtInMs;

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
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedAtInMs() {
        return createdAtInMs;
    }

    public void setCreatedAtInMs(String createdAtInMs) {
        this.createdAtInMs = createdAtInMs;
    }

    public String getLastRetriedAtInMs() {
        return lastRetriedAtInMs;
    }

    public void setLastRetriedAtInMs(String lastRetriedAtInMs) {
        this.lastRetriedAtInMs = lastRetriedAtInMs;
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
}

enum JobStatus {
    pending,
    processing,
    completed,
    failed,
    dead
}

enum JobType {
    send_email,
    call_weather,
    image_processing
}