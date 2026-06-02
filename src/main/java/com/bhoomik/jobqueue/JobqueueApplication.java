package com.bhoomik.jobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobqueueApplication {
    public static void main(String[] args) {
        // Asia/Calcutta is a deprecated IANA alias that PostgreSQL rejects; Kolkata is the accepted name
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(JobqueueApplication.class, args);
    }
}
