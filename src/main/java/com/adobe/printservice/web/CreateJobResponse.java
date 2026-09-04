package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;

import java.time.Instant;

public record CreateJobResponse(String id, JobStatus status, Instant createdAt, Instant updatedAt) {

    public static CreateJobResponse from(Job job) {
        return new CreateJobResponse(job.getId(), job.getStatus(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
