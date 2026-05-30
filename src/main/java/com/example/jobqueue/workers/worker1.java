// package com.example.jobqueue.workers;

// import com.example.jobqueue.EmailRequest;
// import com.example.jobqueue.Job;
// import com.example.jobqueue.JobRepository;
// import com.example.jobqueue.JobStatus;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import java.util.List;

// @Component
// public class worker1 {

//     private final JobRepository jobRepository;
//     private final EmailRequest emailRequest;
//     public worker1(JobRepository jobRepository, EmailRequest emailRequest){
//         this.jobRepository = jobRepository;
//         this.emailRequest = emailRequest;
//     }

//     @Scheduled(fixedDelay = 15000)
//     public void worker()
//     {
//         System.out.println("Worker 1");
//         while(true)
//         {
//             processingPendingJobs();
//         }
//     }

//     @Transactional
//     public void processingPendingJobs()
//     {
//             List<Job> pendingJobs = jobRepository.fetchAndLockPendingJobs(10);
//             for(Job job: pendingJobs)
//             {
//                 job.setStatus(JobStatus.completed);
//                 jobRepository.save(job);
//             }
//             System.out.println("Worker 1 logs: " + pendingJobs);
//     }
// }
