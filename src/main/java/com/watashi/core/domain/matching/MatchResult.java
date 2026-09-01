package com.watashi.core.domain.matching;

import com.watashi.core.domain.common.Skill;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record MatchResult(
        String jobId,
        double overallScore,
        ScoreBreakdown breakdown,
        Set<Skill> matchedSkills,
        Set<Skill> missingRequiredSkills,
        Set<Skill> missingOptionalSkills,
        List<String> conflicts,
        MatchStatus status) {
    public MatchResult {
        Objects.requireNonNull(jobId, "jobId must not be null");
        matchedSkills = matchedSkills == null ? Set.of() : Set.copyOf(matchedSkills);
        missingRequiredSkills = missingRequiredSkills == null ? Set.of() : Set.copyOf(missingRequiredSkills);
        missingOptionalSkills = missingOptionalSkills == null ? Set.of() : Set.copyOf(missingOptionalSkills);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        status = status == null ? MatchStatus.REJECTED : status;
    }
}
