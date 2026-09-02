package com.watashi.core.service;

import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
import com.watashi.core.ports.out.JobApplicationRepository;
import com.watashi.core.ports.out.JobRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DefaultTrackJobApplicationService implements TrackJobApplicationUseCase {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public DefaultTrackJobApplicationService(JobApplicationRepository applicationRepository) {
        this(applicationRepository, null);
    }

    public DefaultTrackJobApplicationService(
            JobApplicationRepository applicationRepository, JobRepository jobRepository) {
        this.applicationRepository =
                Objects.requireNonNull(applicationRepository, "applicationRepository cannot be null");
        this.jobRepository = jobRepository;
    }

    @Override
    public void updateJobStatus(String jobId, JobStatus status) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        applicationRepository.updateJobStatus(jobId, status);

        if (jobRepository != null) {
            jobRepository.findById(jobId).ifPresent(job -> {
                JobOpportunity updated = new JobOpportunity(
                        job.id(),
                        job.title(),
                        job.company(),
                        job.description(),
                        job.requiredSkills(),
                        job.optionalSkills(),
                        job.seniorityLevel(),
                        job.workMode(),
                        job.location(),
                        job.salaryRange(),
                        job.sourceUrl(),
                        status);
                jobRepository.save(updated);
            });
        }
    }

    @Override
    public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {
        Objects.requireNonNull(jobIds, "jobIds cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        for (String jobId : jobIds) {
            updateJobStatus(jobId, status);
        }
    }

    @Override
    public Map<String, JobStatus> getApplicationHistory() {
        return applicationRepository.loadHistory();
    }

    @Override
    public Optional<JobStatus> getJobStatus(String jobId) {
        if (jobId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(applicationRepository.loadHistory().get(jobId));
    }
}
