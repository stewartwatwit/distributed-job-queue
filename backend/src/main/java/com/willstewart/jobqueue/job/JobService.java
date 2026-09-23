package com.willstewart.jobqueue.job;

import org.springframework.stereotype.Service;
import com.willstewart.jobqueue.dto.JobRequest;
import com.willstewart.jobqueue.job.JobStatus.Status;

import java.util.UUID;

@Service
public class JobService {

    public static final int MAX_ATTEMPTS = 5;

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void submitJob(JobRequest jobRequest) {

        Job job = new Job(jobRequest);

        jobRepository.save(job);
    }

    public Job getJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> {
            // eventually will be JobNotFoundException -> 404
            throw new RuntimeException("Job not found with ID: " + jobId);
        });
    }

    public void claimNextJob() {
        
    }

    public void completeJob(UUID jobID) {
        Job job = jobRepository.findById(jobID).orElseThrow(() -> {
            // eventually will be JobNotFoundException -> 404
            throw new RuntimeException("Job not found with ID: " + jobID);
        });

        if(job.getStatus() != Status.RUNNING) {
            throw new RuntimeException("Cannot complete a job that is not running");
        }
        
        job.setStatus(Status.COMPLETED);
        job.setEndedAt(java.time.Instant.now());

        jobRepository.save(job);
    }

    public void failJob(UUID jobID) {
        Job job = jobRepository.findById(jobID).orElseThrow(() -> {
            // eventually will be JobNotFoundException -> 404
            throw new RuntimeException("Job not found with ID: " + jobID);
        });

        int currAttempt = job.getAttemptCount();

        if(job.getStatus() != Status.RUNNING) {
            throw new RuntimeException("Cannot fail a job that is not running");
        }
        if(currAttempt >= MAX_ATTEMPTS) {
            job.setStatus(Status.FAILED);
            jobRepository.save(job);
            return;
        }

        job.setStatus(Status.PENDING);

        jobRepository.save(job);
    }
}
