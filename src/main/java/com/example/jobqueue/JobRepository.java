package com.example.jobqueue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = """
    SELECT * FROM jobs
    WHERE status = 'PENDING'
    FOR UPDATE SKIP LOCKED
    LIMIT 10
    """, nativeQuery = true)
    List<Job> fetchPendingJobs();
}
