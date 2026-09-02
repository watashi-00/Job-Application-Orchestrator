package com.watashi.core.ports.in;

import com.watashi.core.domain.job.JobOpportunity;
import java.util.List;

@FunctionalInterface
public interface GetJobsUseCase {
    List<JobOpportunity> getJobs();
}
