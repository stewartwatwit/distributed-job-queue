package com.willstewart.jobqueue.worker;

import com.willstewart.jobqueue.job.Job;
import com.willstewart.jobqueue.job.JobService;

public class Worker implements Runnable {
    
    private final JobService jobService;

    public Worker(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Job job = jobService.claimNextJob().orElse(null);

            if(job == null) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            try {
                processJob(job);

                jobService.completeJob(job.getId());
            } catch (Exception e) {
                jobService.failJob(job.getId());
            }
        }
    }

    private void processJob(Job job) {
        System.out.println("Processing job: " + job.getId());
    }
}
