package com.willstewart.jobqueue.job;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.willstewart.jobqueue.job.JobStatus.Status;
import com.willstewart.jobqueue.job.JobType.Type;
import com.willstewart.jobqueue.dto.JobRequest;

@Entity
@Table(name= "jobs")
public class Job {

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    // time job is created
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    // time a job is updated
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(nullable = false)
    private int priority;

    @Column(name = "attempt_count")
    private int attemptCount = 0;

    protected Job() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = now();
    }

    public Job(JobRequest request) {
        this.type = request.getType();
        this.status = Status.PENDING;
        this.payload = request.getPayload();
        this.priority = request.getPriority();
    }

    public UUID getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

     public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
}
