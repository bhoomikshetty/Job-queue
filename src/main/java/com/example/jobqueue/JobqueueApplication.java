package com.example.jobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobqueueApplication {
	public static void main(String[] args) {
		
		System.out.println("java.util.TimeZone.getDefault()");
		System.out.println(java.util.TimeZone.getDefault());
		SpringApplication app = new SpringApplication(JobqueueApplication.class);
		app.run(args);
	}
}
