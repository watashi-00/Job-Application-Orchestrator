package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import java.util.List;

public interface AssessJobCompatibilityUseCase {
    MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config);

    List<MatchResult> evaluateAll(List<JobOpportunity> jobs, CandidateProfile profile, FilterConfiguration config);
}
