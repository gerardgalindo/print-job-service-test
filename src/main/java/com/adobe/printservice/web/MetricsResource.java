package com.adobe.printservice.web;

import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsResource {

    private final JobRepository jobRepository;

    public MetricsResource(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("total", jobRepository.count());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            byStatus.put(status.name(), jobRepository.countByStatus(status));
        }
        metrics.put("byStatus", byStatus);

        return metrics;
    }
}
