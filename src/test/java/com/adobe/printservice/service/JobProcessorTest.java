package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class JobProcessorTest {

    private static final String INVOICE_TEMPLATE_ID = "b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10";

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void processJob_successfulRender_reachesDone() {
        Job job = createQueuedJob();

        JobProcessor processor = new JobProcessor(jobRepository) {
            @Override
            protected String render(Job job) {
                return "Rendered content";
            }
        };

        processor.processJob(job.getId());

        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.DONE, updatedJob.getStatus());
        assertEquals("Rendered content", updatedJob.getResultContent());
        assertEquals(1, updatedJob.getAttempts());
        assertNull(updatedJob.getErrorMessage());
    }

    @Test
    void processJob_transientFailure_retriesAndEventuallySucceeds() {
        Job job = createQueuedJob();

        JobProcessor processor = new JobProcessor(jobRepository) {
            private int callCount = 0;

            @Override
            protected String render(Job job) {
                callCount++;
                if (callCount <= 2) {
                    throw new RuntimeException("Transient failure on attempt " + callCount);
                }
                return "Rendered after retries";
            }
        };

        processor.processJob(job.getId());

        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.DONE, updatedJob.getStatus());
        assertEquals("Rendered after retries", updatedJob.getResultContent());
        assertEquals(3, updatedJob.getAttempts());
    }

    @Test
    void processJob_permanentFailure_reachesFailedAfterMaxAttempts() {
        Job job = createQueuedJob();

        JobProcessor processor = new JobProcessor(jobRepository) {
            @Override
            protected String render(Job job) {
                throw new RuntimeException("Permanent rendering failure");
            }
        };

        processor.processJob(job.getId());

        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.FAILED, updatedJob.getStatus());
        assertEquals(JobProcessor.MAX_ATTEMPTS, updatedJob.getAttempts());
        assertEquals("Permanent rendering failure", updatedJob.getErrorMessage());
        assertNull(updatedJob.getResultContent());
    }

    @Test
    void processJob_jobNotFound_doesNotThrow() {
        JobProcessor processor = new JobProcessor(jobRepository);

        processor.processJob("nonexistent-id");

        assertEquals(0, jobRepository.count());
    }


    private Job createQueuedJob() {
        Job job = new Job();
        job.setTemplateId(INVOICE_TEMPLATE_ID);
        job.setStatus(JobStatus.QUEUED);
        return jobRepository.save(job);
    }
}
