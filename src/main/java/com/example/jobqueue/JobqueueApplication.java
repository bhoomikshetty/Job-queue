package com.example.jobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobqueueApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(JobqueueApplication.class);
		app.run(args);
	}
}
