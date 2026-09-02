package com.watashi.core.service;

import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.domain.application.CandidateCredential;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.out.CandidateCredentialsRepository;
import com.watashi.core.ports.out.JobApplicationRepository;
import com.watashi.core.ports.out.JobRepository;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultDispatchJobApplicationService implements DispatchJobApplicationUseCase {

    private static final Set<String> DEFAULT_AUTH_REQUIRED_DOMAINS =
            Set.of("workday.com", "linkedin.com", "lever.co", "greenhouse.io");

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final CandidateCredentialsRepository credentialsRepository;
    private final Set<String> authRequiredDomains;
    private final List<ApplicationDispatchLog> dispatchLogs = new CopyOnWriteArrayList<>();

    public DefaultDispatchJobApplicationService(
            JobRepository jobRepository,
            JobApplicationRepository applicationRepository,
            CandidateCredentialsRepository credentialsRepository) {
        this(jobRepository, applicationRepository, credentialsRepository, DEFAULT_AUTH_REQUIRED_DOMAINS);
    }

    public DefaultDispatchJobApplicationService(
            JobRepository jobRepository,
            JobApplicationRepository applicationRepository,
            CandidateCredentialsRepository credentialsRepository,
            Set<String> authRequiredDomains) {
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository cannot be null");
        this.applicationRepository =
                Objects.requireNonNull(applicationRepository, "applicationRepository cannot be null");
        this.credentialsRepository =
                Objects.requireNonNull(credentialsRepository, "credentialsRepository cannot be null");
        this.authRequiredDomains =
                authRequiredDomains == null ? DEFAULT_AUTH_REQUIRED_DOMAINS : Set.copyOf(authRequiredDomains);
    }

    @Override
    public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
        Objects.requireNonNull(jobId, "jobId cannot be null");

        JobOpportunity job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        String domain = extractDomain(job.sourceUrl());
        boolean isAuthRequired = isAuthRequiredDomain(domain);
        boolean hasCredentials = credentialsRepository.findByDomain(domain).isPresent();
        boolean requiresHumanAssistance = isAuthRequired && !hasCredentials;

        String status;
        String message;

        if (requiresHumanAssistance) {
            status = "NEEDS_HUMAN_ASSISTANCE";
            message = "Human assistance required: missing credentials for domain " + domain;
        } else {
            status = "DISPATCHED_SUCCESS";
            message = "Application successfully dispatched to " + job.company()
                    + (sendCoverLetter ? " (with cover letter)" : "");

            applicationRepository.updateJobStatus(jobId, JobStatus.APPLIED);
            JobOpportunity updatedJob = new JobOpportunity(
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
                    JobStatus.APPLIED);
            jobRepository.save(updatedJob);
        }

        ApplicationDispatchLog log = new ApplicationDispatchLog(
                jobId,
                job.title(),
                job.company(),
                status,
                message,
                LocalDateTime.now().toString(),
                requiresHumanAssistance,
                domain);

        dispatchLogs.add(log);
        return log;
    }

    @Override
    public List<ApplicationDispatchLog> getDispatchLogs() {
        return List.copyOf(dispatchLogs);
    }

    @Override
    public void saveDomainCredential(String domain, String username, String token) {
        Objects.requireNonNull(domain, "domain cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(token, "token cannot be null");

        CandidateCredential credential = new CandidateCredential(domain, username, token, LocalDateTime.now());
        credentialsRepository.saveCredential(credential);
    }

    private boolean isAuthRequiredDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        String cleanDomain = domain.toLowerCase();
        return authRequiredDomains.stream()
                .anyMatch(d -> cleanDomain.equalsIgnoreCase(d) || cleanDomain.endsWith("." + d.toLowerCase()));
    }

    private String extractDomain(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return "direct";
        }
        try {
            URI uri = URI.create(sourceUrl);
            String host = uri.getHost();
            if (host == null) {
                return "direct";
            }
            host = host.toLowerCase();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            if (host.startsWith("jobs.")) {
                host = host.substring(5);
            }
            return host;
        } catch (Exception e) {
            return "direct";
        }
    }
}
