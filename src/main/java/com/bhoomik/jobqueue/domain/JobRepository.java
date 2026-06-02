package com.bhoomik.jobqueue.domain;

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

    @Query(value = """
    UPDATE jobs.jobs
    SET status = 'processing',
        locked_at = :lockedAt
    WHERE id IN (
        SELECT id FROM jobs.jobs
        WHERE status = 'pending'
        AND id = :id
        FOR UPDATE SKIP LOCKED
    )
    RETURNING *
    """, nativeQuery = true)
    Optional<Job> findByIdAndLock(@Param("id") Long id, @Param("lockedAt") OffsetDateTime lockedAt);

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
        SET retries = :retries,
            last_retried_at = :lastRetriedAt,
            error_message = :errorMessage,
            last_error_at = :lastErrorAt,
            next_retry_at = :nextRetryAt,
            status = :status
        WHERE id = :id
    """, nativeQuery = true)
    void updateJob(
        @Param("id") Long id,
        @Param("retries") int retries,
        @Param("lastRetriedAt") OffsetDateTime lastRetriedAt,
        @Param("errorMessage") String errorMessage,
        @Param("lastErrorAt") OffsetDateTime lastErrorAt,
        @Param("nextRetryAt") OffsetDateTime nextRetryAt,
        @Param("status") String status
    );

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

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE jobs.jobs
    SET status = 'pending',
        locked_at = NULL
    WHERE status = 'processing' AND locked_at <= :now
    """, nativeQuery = true)
    int resetStuckProcessingJobs(@Param("now") OffsetDateTime now);
}
