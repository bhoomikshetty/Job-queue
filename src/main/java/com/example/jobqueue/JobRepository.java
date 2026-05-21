package com.example.jobqueue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = """
    SELECT * FROM jobs.jobs
    WHERE status = '0'
    ORDER BY id ASC
    LIMIT 10
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Job> fetchPendingJobs(int limit);

    // @Modifying
    @Query(value = """
    UPDATE jobs.jobs
    SET status = '1'
    WHERE id IN (
        SELECT id FROM jobs.jobs
        WHERE status = '0'
        ORDER BY id
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    )
    RETURNING *
    """, nativeQuery = true)
    List<Job> fetchAndLockPendingJobs(@Param("limit") int limit);


    @Query(value = """
    SELECT * FROM jobs.jobs
    WHERE status = :status
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Job> fetchAllJobsByStatus(@Param("status") String status);

}
