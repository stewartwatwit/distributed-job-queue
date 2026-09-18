package com.willstewart.jobqueue.job;

public class JobStatus {
     public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
