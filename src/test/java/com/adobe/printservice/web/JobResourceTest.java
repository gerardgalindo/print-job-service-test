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
                .andExpect(jsonPath("$[0].errorMessage").value(failedJob.getErrorMessage()));
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
}
