package com.willstewart.jobqueue.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import java.util.Optional;


public interface JobRepository extends JpaRepository<Job, UUID> {
    
    @Query(value = """
            SELECT *
            FROM jobs
            WHERE status = 'PENDING'
            ORDER BY priority DESC, created_at ASC, id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Job> findAndLockNextPendingJob();
}
