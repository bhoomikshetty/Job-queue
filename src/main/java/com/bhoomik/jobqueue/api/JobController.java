package com.bhoomik.jobqueue.api;

import com.bhoomik.jobqueue.domain.Job;
import com.bhoomik.jobqueue.domain.JobRepository;
import com.bhoomik.jobqueue.domain.JobStatus;
import com.bhoomik.jobqueue.scheduler.RedisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class JobController {

    final JobRepository jobRepository;
    final RedisService redisService;

    public JobController(JobRepository jobRepository, RedisService redisService) {
        this.jobRepository = jobRepository;
        this.redisService = redisService;
    }

    @PostMapping("/addJob")
    public ResponseEntity<Job> addJob(@RequestBody Job job) {
        System.out.println(job.toString());
        Job saved = jobRepository.save(job);

        try {
            redisService.addJobToRedis(saved);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/getJobStatus/")
    public JobStatus getJobStatus(@RequestParam Long id) {
        try {
            Optional<Job> job = jobRepository.findById(id);
            return job.get().getStatus();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/getAllJobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        try {
            List<Job> jobs = jobRepository.findAll();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/getJobById")
    public ResponseEntity<Job> getJobById(@RequestParam Long id) {
        try {
            Optional<Job> job = jobRepository.findById(id);
            return job.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/getPendingJobs")
    public ResponseEntity<List<Job>> getJobsByStatus(@RequestParam String status) {
        try {
            List<Job> jobs = jobRepository.fetchAllJobsByStatus(status.toUpperCase());
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
