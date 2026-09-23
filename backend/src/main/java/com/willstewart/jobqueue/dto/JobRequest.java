package com.willstewart.jobqueue.dto;

import jakarta.validation.constraints.NotNull;

import com.willstewart.jobqueue.job.JobType.Type;

public class JobRequest {
    @NotNull
    private Type type;

    @NotNull
    private String payload;

    @NotNull
    private Integer priority;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}