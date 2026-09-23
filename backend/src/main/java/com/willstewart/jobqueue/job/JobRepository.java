package com.willstewart.jobqueue.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import java.util.Optional;


public interface JobRepository extends JpaRepository<Job, UUID> {
    
    @Query("SELECT j FROM Job j WHERE j.status = com.willstewart.jobqueue.job.JobStatus.Status.PENDING ORDER BY j.priority DESC, j.createdAt ASC, j.id ASC LIMIT 1")
    Optional<Job> findNextPendingJob();
}
