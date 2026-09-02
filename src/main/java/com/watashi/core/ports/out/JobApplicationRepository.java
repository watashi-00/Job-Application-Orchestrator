package com.watashi.core.ports.out;

import com.watashi.core.domain.job.JobStatus;
import java.util.Map;

public interface JobApplicationRepository {
    Map<String, JobStatus> loadHistory();

    void saveHistory(Map<String, JobStatus> history);

    void updateJobStatus(String jobId, JobStatus status);
}
