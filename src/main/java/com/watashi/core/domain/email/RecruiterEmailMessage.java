package com.watashi.core.domain.email;

import com.watashi.core.domain.job.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record RecruiterEmailMessage(
        String id,
        String sender,
        String recipient,
        String subject,
        String bodyText,
        Instant receivedAt,
        String matchedJobId,
        String matchedCompany,
        JobStatus detectedStatus) {

    public RecruiterEmailMessage {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        sender = sender == null ? "" : sender;
        recipient = recipient == null ? "" : recipient;
        subject = subject == null ? "" : subject;
        bodyText = bodyText == null ? "" : bodyText;
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public RecruiterEmailMessage(String sender, String recipient, String subject, String bodyText) {
        this(UUID.randomUUID().toString(), sender, recipient, subject, bodyText, Instant.now(), null, null, null);
    }

    public RecruiterEmailMessage withMatch(String jobId, String company, JobStatus status) {
        return new RecruiterEmailMessage(id, sender, recipient, subject, bodyText, receivedAt, jobId, company, status);
    }
}
