package com.example.jobqueue;

import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.List;

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
    public static JobStatus getJobStatus(Long id){
        Optional<Job> job = jobRepository.findById(id);
        return job.get().status;
    }


    @GetMapping("/testEndpoint")
    public static ResponseEntity testEndpoint(){
        List<Job> jobs = jobRepository.fetchPendingJobs();
        return ResponseEntity.status(200).body(jobs);
    }
}


