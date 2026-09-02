package com.watashi.core.ports.out;

import com.watashi.core.domain.job.JobOpportunity;
import java.util.List;
import java.util.Optional;

public interface JobRepository {
    void save(JobOpportunity job);

    List<JobOpportunity> findAll();

    Optional<JobOpportunity> findById(String id);
}
