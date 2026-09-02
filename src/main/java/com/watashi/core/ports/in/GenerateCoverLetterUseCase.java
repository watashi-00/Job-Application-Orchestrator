package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.CoverLetterResult;
import com.watashi.core.domain.matching.MatchResult;

public interface GenerateCoverLetterUseCase {
    CoverLetterResult generateCoverLetter(JobOpportunity job, CandidateProfile profile, MatchResult match);
}
