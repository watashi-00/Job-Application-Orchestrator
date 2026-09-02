package com.watashi.core.domain.matching;

import java.util.Objects;

public record CoverLetterResult(
        String jobId, String jobTitle, String company, String coverLetter, String matchExplanation) {
    public CoverLetterResult {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(jobTitle, "jobTitle must not be null");
        Objects.requireNonNull(company, "company must not be null");
        Objects.requireNonNull(coverLetter, "coverLetter must not be null");
        Objects.requireNonNull(matchExplanation, "matchExplanation must not be null");
    }
}
