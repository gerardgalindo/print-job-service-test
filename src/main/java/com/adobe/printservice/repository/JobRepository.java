package com.adobe.printservice.repository;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);
}
