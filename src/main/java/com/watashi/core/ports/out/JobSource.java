package com.watashi.core.ports.out;

import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import java.util.List;

public interface JobSource {

    String getSourceName();

    List<JobOpportunity> fetchJobs(JobQuery query);
}
