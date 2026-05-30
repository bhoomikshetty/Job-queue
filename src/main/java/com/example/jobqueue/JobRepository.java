package com.example.jobqueue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = """
    SELECT * FROM jobs.jobs
    WHERE status = 'pending'
    ORDER BY id ASC
    LIMIT 10
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Job> fetchPendingJobs(int limit);

    // @Modifying
    @Query(value = """
    UPDATE jobs.jobs
    SET status = 'processing'
    WHERE id IN (
        SELECT id FROM jobs.jobs
        WHERE status = 'pending'
        ORDER BY id
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    )
    RETURNING *
    """, nativeQuery = true)
    List<Job> fetchAndLockPendingJobs(@Param("limit") int limit);

    // @Transactional
    // @Modifying
    @Query(value = """
    UPDATE jobs.jobs
    SET status = 'processing'
    WHERE id IN (
        SELECT id FROM jobs.jobs
        WHERE status = 'pending'
        AND id = :id
        FOR UPDATE SKIP LOCKED
    )
    RETURNING *
    """, nativeQuery = true)
    Optional<Job> findByIdAndLock(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs.jobs
        SET status = :status
        WHERE id = :id
    """, nativeQuery = true)
    void updateJobStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs.jobs
        SET retries = retries + 1
        WHERE id = :id
    """, nativeQuery = true)
    void incrementRetries(@Param("id") Long id);

    @Query(value = """
    SELECT * FROM jobs.jobs
    WHERE status = :status
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Job> fetchAllJobsByStatus(@Param("status") String status);

    @Query(value = """
    SELECT * FROM jobs.jobs    
    WHERE status = 'pending' AND scheduled_at <= :startTime
    """, nativeQuery = true)
    List<Job> fetchScheduledJobs(@Param("startTime") OffsetDateTime startTime);

    
    
}
