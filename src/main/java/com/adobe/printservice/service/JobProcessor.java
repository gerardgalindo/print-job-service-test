package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);
    static final int MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;

    public JobProcessor(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Async
    public void processJob(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job not found: {}", jobId);
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        for (int attempts = 1; attempts <= MAX_ATTEMPTS; attempts++) {
            job.setAttempts(attempts);
            try {
                String result = render(job);
                job.setResultContent(result);
                job.setStatus(JobStatus.DONE);
                job.setErrorMessage(null);
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
                log.info("Job {} completed successfully on attempt {}", jobId, attempts);
                return;
            } catch (Exception e) {
                job.setErrorMessage(e.getMessage());
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
                log.warn("Job {} failed on attempt {}: {}", jobId, attempts, e.getMessage());
            }
        }

        job.setStatus(JobStatus.FAILED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        log.error("Job {} failed after {} attempts", jobId, MAX_ATTEMPTS);
    }

    protected String render(Job job) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rendering interrupted", e);
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.5) {
            throw new RuntimeException("Transient rendering failure");
        }

        return "Rendered content for template " + job.getTemplateId();
    }
}
