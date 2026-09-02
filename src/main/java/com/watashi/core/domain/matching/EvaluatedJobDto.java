package com.watashi.core.domain.matching;

import com.watashi.core.domain.job.JobOpportunity;
import java.util.Objects;

public record EvaluatedJobDto(JobOpportunity job, MatchResult match) {
    public EvaluatedJobDto {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(match, "match cannot be null");
    }
}
