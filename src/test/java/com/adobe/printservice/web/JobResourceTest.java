package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    private Job queuedJob;
    private Job failedJob;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();

        queuedJob = new Job();
        queuedJob.setTemplateId("b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10");
        queuedJob.setStatus(JobStatus.QUEUED);
        jobRepository.save(queuedJob);

        failedJob = new Job();
        failedJob.setTemplateId("b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10");
        failedJob.setStatus(JobStatus.FAILED);
        failedJob.setErrorMessage("Simulated failure");
        failedJob.setAttempts(3);
        jobRepository.save(failedJob);
    }

    @Test
    void getJobs_returnsAllJobs() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(queuedJob.getId(), failedJob.getId())))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("QUEUED", "FAILED")))
                .andExpect(jsonPath("$[*].templateId", hasItem(queuedJob.getTemplateId())));
    }

    @Test
    void getJobs_filterByStatus_returnsMatchingJobs() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(failedJob.getId()))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].templateId").value(failedJob.getTemplateId()))
                .andExpect(jsonPath("$[0].errorMessage").value(failedJob.getErrorMessage()))
                .andExpect(jsonPath("$[0].attempts").value(failedJob.getAttempts()));
    }

    @Test
    void getJob_existingId_returns200() throws Exception {
        mockMvc.perform(get("/jobs/{id}", queuedJob.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(queuedJob.getId()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.templateId").value(queuedJob.getTemplateId()));
    }

    @Test
    void getJob_missingId_returns404() throws Exception {
        mockMvc.perform(get("/jobs/{id}", "nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResult_doneJob_returns200() throws Exception {
        Job doneJob = new Job();
        doneJob.setTemplateId("b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10");
        doneJob.setStatus(JobStatus.DONE);
        String content = "Rendered invoice content";
        doneJob.setResultContent(content);
        jobRepository.save(doneJob);

        mockMvc.perform(get("/jobs/{id}/result", doneJob.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(content));
    }

    @Test
    void getResult_queuedJob_returns409() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", queuedJob.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Job is still processing: " + queuedJob.getId()));
    }

    @Test
    void getResult_processingJob_returns409() throws Exception {
        Job processingJob = new Job();
        processingJob.setTemplateId("b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10");
        processingJob.setStatus(JobStatus.PROCESSING);
        jobRepository.save(processingJob);

        mockMvc.perform(get("/jobs/{id}/result", processingJob.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Job is still processing: " + processingJob.getId()));
    }

    @Test
    void getResult_failedJob_returns404() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", failedJob.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No result available for job: " + failedJob.getId()));
    }

    @Test
    void getResult_missingJob_returns404() throws Exception {
        mockMvc.perform(get("/jobs/{id}/result", "nonexistent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found: nonexistent-id"));
    }
}
