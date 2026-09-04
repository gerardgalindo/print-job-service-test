package com.adobe.printservice.web;

import com.adobe.printservice.exception.InvalidJobRequestException;
import com.adobe.printservice.exception.JobStillProcessingException;
import com.adobe.printservice.exception.ResourceNotFoundException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.service.JobProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final JobProcessor jobProcessor;

    public JobResource(JobRepository jobRepository,
                       RenderTemplateRepository renderTemplateRepository,
                       JobProcessor jobProcessor) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.jobProcessor = jobProcessor;
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody CreateJobRequest request) {
        if (request.templateId() == null || request.templateId().isBlank()) {
            throw new InvalidJobRequestException("templateId is required");
        }

        renderTemplateRepository.findById(request.templateId())
                .orElseThrow(() -> new InvalidJobRequestException(
                        "Template not found: " + request.templateId()));

        Job job = new Job();
        job.setTemplateId(request.templateId());
        job.setParameters(request.parameters());
        jobRepository.save(job);

        jobProcessor.processJob(job.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping
    public List<Job> getJobs(@RequestParam(required = false) JobStatus status) {
        if (status != null) {
            return jobRepository.findByStatus(status);
        }
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }

    @GetMapping("/{id}/result")
    public String getResult(@PathVariable String id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        if (job.getStatus() == JobStatus.DONE) {
            return job.getResultContent();
        }
        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.PROCESSING) {
            throw new JobStillProcessingException("Job is still processing: " + id);
        }
        throw new ResourceNotFoundException("No result available for job: " + id);
    }
}
