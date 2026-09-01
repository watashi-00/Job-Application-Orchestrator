package com.watashi.core.domain.candidate;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import java.util.Set;

public record CandidatePreferences(
        SalaryRange desiredSalary,
        Set<WorkMode> preferredWorkModes,
        Set<SeniorityLevel> targetSeniorities,
        Set<String> preferredLocations) {

    public CandidatePreferences {
        preferredWorkModes = preferredWorkModes == null ? Set.of() : Set.copyOf(preferredWorkModes);
        targetSeniorities = targetSeniorities == null ? Set.of() : Set.copyOf(targetSeniorities);
        preferredLocations = preferredLocations == null ? Set.of() : Set.copyOf(preferredLocations);
    }
}
