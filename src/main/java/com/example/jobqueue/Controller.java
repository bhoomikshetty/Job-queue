package com.example.jobqueue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;

@RestController
public class Controller {

    public static JobRepository jobRepository;

    public Controller(JobRepository jobRepository)
    {
        Controller.jobRepository = jobRepository;
    }

    @PostMapping("/addJob")
    public static ResponseEntity<Job> addJob(@RequestBody Job job){
        System.out.println(job.toString());
        Job saved = jobRepository.save(job);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/getJobStatus")
    public static JobStatus getJobStatus(@RequestParam Long id){
        Optional<Job> job = jobRepository.findById(id);
        return job.get().status;
    }

    @GetMapping("/getAllJobs")
    public static ResponseEntity<List<Job>> getAllJobs(){
        List<Job> jobs = jobRepository.findAll();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/getJobById")
    public static ResponseEntity<Job> getJobById(@RequestParam Long id){
        Optional<Job> job = jobRepository.findById(id);
        return job.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/getPendingJobs")
    public static ResponseEntity testEndpoint(@RequestParam String status){
        List<Job> jobs = jobRepository.fetchAllJobsByStatus(status);
        return ResponseEntity.status(200).body(jobs);
    }
}


