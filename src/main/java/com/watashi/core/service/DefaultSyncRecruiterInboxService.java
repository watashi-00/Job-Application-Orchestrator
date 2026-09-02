package com.watashi.core.service;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DefaultSyncRecruiterInboxService implements SyncRecruiterInboxUseCase {

    private final RecruiterEmailRepository emailRepository;
    private final JobRepository jobRepository;

    public DefaultSyncRecruiterInboxService(RecruiterEmailRepository emailRepository, JobRepository jobRepository) {
        this.emailRepository = Objects.requireNonNull(emailRepository, "emailRepository cannot be null");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository cannot be null");
    }

    @Override
    public RecruiterEmailMessage receiveIncomingEmail(
            String sender, String recipient, String subject, String bodyText) {
        String safeSender = sender == null ? "" : sender;
        String safeSubject = subject == null ? "" : subject;
        String safeBody = bodyText == null ? "" : bodyText;

        String fullText = (safeSender + " " + safeSubject + " " + safeBody).toLowerCase();

        Optional<JobOpportunity> matchedJob = findMatchingJob(fullText);

        String matchedJobId = matchedJob.map(JobOpportunity::id).orElse(null);
        String matchedCompany = matchedJob.map(JobOpportunity::company).orElse(null);
        JobStatus detectedStatus = detectStatus(safeSubject, safeBody);

        if (matchedJob.isPresent() && detectedStatus != null) {
            JobOpportunity job = matchedJob.get();
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
                    detectedStatus);
            jobRepository.save(updatedJob);
        }

        RecruiterEmailMessage emailMessage = new RecruiterEmailMessage(safeSender, recipient, safeSubject, safeBody)
                .withMatch(matchedJobId, matchedCompany, detectedStatus);

        emailRepository.save(emailMessage);
        return emailMessage;
    }

    @Override
    public List<RecruiterEmailMessage> getAllReceivedEmails() {
        return emailRepository.findAll();
    }

    private Optional<JobOpportunity> findMatchingJob(String searchableText) {
        List<JobOpportunity> jobs = jobRepository.findAll();
        for (JobOpportunity job : jobs) {
            if (job.company() != null && !job.company().isBlank()) {
                String companyLower = job.company().trim().toLowerCase();
                if (searchableText.contains(companyLower)) {
                    return Optional.of(job);
                }
            }
        }
        return Optional.empty();
    }

    private JobStatus detectStatus(String subject, String bodyText) {
        String combined = (subject + " " + bodyText).toLowerCase();

        if (combined.contains("offer")
                || combined.contains("pleased to offer")
                || combined.contains("congratulations")) {
            return JobStatus.OFFER;
        }

        if (combined.contains("interview")
                || combined.contains("schedule")
                || combined.contains("screening")
                || combined.contains("next round")
                || combined.contains("assessment")
                || combined.contains("invitation")) {
            return JobStatus.INTERVIEWING;
        }

        if (combined.contains("reject")
                || combined.contains("not moving forward")
                || combined.contains("unfortunately")
                || combined.contains("other candidates")
                || combined.contains("decided not to proceed")) {
            return JobStatus.REJECTED;
        }

        return null;
    }
}
