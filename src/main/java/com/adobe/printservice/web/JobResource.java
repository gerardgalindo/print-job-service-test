package com.adobe.printservice.web;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobRepository jobRepository;

    public JobResource(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public List<Job> getJobs(@RequestParam(required = false) JobStatus status) {
        if (status != null) {
            return jobRepository.findByStatus(status);
        }
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable String id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
