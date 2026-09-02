package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import java.util.List;

public interface DiscoverJobsUseCase {
    List<MatchResult> discoverAndEvaluate(CandidateProfile profile, FilterConfiguration config);
}
