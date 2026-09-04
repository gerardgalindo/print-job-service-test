package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetricsResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void getMetrics_emptyDatabase_returnsZeroCounts() throws Exception {
        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.byStatus.QUEUED").value(0))
                .andExpect(jsonPath("$.byStatus.PROCESSING").value(0))
                .andExpect(jsonPath("$.byStatus.DONE").value(0))
                .andExpect(jsonPath("$.byStatus.FAILED").value(0));
    }

    @Test
    void getMetrics_withJobs_returnsCorrectCounts() throws Exception {
        createJobWithStatus(JobStatus.QUEUED);
        createJobWithStatus(JobStatus.QUEUED);
        createJobWithStatus(JobStatus.DONE);
        createJobWithStatus(JobStatus.DONE);
        createJobWithStatus(JobStatus.DONE);
        createJobWithStatus(JobStatus.FAILED);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.byStatus.QUEUED").value(2))
                .andExpect(jsonPath("$.byStatus.PROCESSING").value(0))
                .andExpect(jsonPath("$.byStatus.DONE").value(3))
                .andExpect(jsonPath("$.byStatus.FAILED").value(1));
    }

    private void createJobWithStatus(JobStatus status) {
        Job job = new Job();
        job.setTemplateId("b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10");
        job.setStatus(status);
        jobRepository.save(job);
    }
}
