package com.example.jobqueue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    static JobRepository jobRepository;

    public Controller(JobRepository jobRepository)
    {
        this.jobRepository = jobRepository;
    }
    @PostMapping("/addJob")
    public static ResponseEntity<Job> addJob(@RequestBody Job job){
        System.out.println(job.toString());
        Job saved = jobRepository.save(job);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/getJobStatus")
    public static JobStatus getJobStatus(){
        return JobStatus.completed;
    }
}


