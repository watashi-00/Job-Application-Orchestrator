package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchingEngine;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultAssessJobCompatibilityService implements AssessJobCompatibilityUseCase {

    private final MatchingEngine matchingEngine;

    public DefaultAssessJobCompatibilityService(MatchingEngine matchingEngine) {
        this.matchingEngine = Objects.requireNonNull(matchingEngine, "matchingEngine cannot be null");
    }

    @Override
    public MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        return matchingEngine.evaluate(job, profile, config);
    }

    @Override
    public List<MatchResult> evaluateAll(
            List<JobOpportunity> jobs, CandidateProfile profile, FilterConfiguration config) {
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }
        return jobs.stream()
                .filter(Objects::nonNull)
                .map(job -> evaluate(job, profile, config))
                .collect(Collectors.toList());
    }
}
