package com.watashi.core.domain.application;

import java.util.Objects;

public record ApplicationDispatchLog(
        String jobId,
        String jobTitle,
        String company,
        String status,
        String message,
        String timestamp,
        boolean requiresHumanAssistance,
        String domain) {

    public ApplicationDispatchLog {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(jobTitle, "jobTitle cannot be null");
        Objects.requireNonNull(company, "company cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(domain, "domain cannot be null");
    }
}
