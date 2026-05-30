// package com.example.jobqueue.workers;

// import com.example.jobqueue.Job;
// import com.example.jobqueue.JobRepository;
// import com.example.jobqueue.JobStatus;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import java.util.List;

// @Component
// public class worker2 {

//     private final JobRepository jobRepository;

//     public worker2(JobRepository jobRepository){
//         this.jobRepository = jobRepository;
//     }

//     // @Scheduled(fixedDelay = 15000)
//     public void worker()
//     {
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
//             System.out.println("Worker 2 logs: " + pendingJobs);
//     }
// }
