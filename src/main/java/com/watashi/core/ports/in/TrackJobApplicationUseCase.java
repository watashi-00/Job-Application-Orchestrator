package com.watashi.core.ports.in;

import com.watashi.core.domain.job.JobStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TrackJobApplicationUseCase {
    void updateJobStatus(String jobId, JobStatus status);

    void batchUpdateJobStatus(List<String> jobIds, JobStatus status);

    Map<String, JobStatus> getApplicationHistory();

    Optional<JobStatus> getJobStatus(String jobId);
}
