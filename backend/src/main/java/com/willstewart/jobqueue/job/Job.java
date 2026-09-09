package com.willstewart.jobqueue.job;

import java.lang.annotation.Inherited;

import javax.annotation.processing.Generated;

import jakarta.persistence.*;

@Entity
@Table(name= "jobs")
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String status;

    protected Job() {
    }

    public Job(String status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
