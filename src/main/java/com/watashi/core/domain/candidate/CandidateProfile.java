package com.watashi.core.domain.candidate;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import java.util.Objects;
import java.util.Set;

public record CandidateProfile(
        String id,
        String title,
        String summary,
        Set<Skill> skills,
        Set<SeniorityLevel> targetSeniorities,
        Set<WorkMode> preferredWorkModes,
        SalaryRange desiredSalary,
        Set<String> preferredLocations) {

    public CandidateProfile {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(title, "title cannot be null");

        skills = skills == null ? Set.of() : Set.copyOf(skills);
        targetSeniorities = targetSeniorities == null ? Set.of() : Set.copyOf(targetSeniorities);
        preferredWorkModes = preferredWorkModes == null ? Set.of() : Set.copyOf(preferredWorkModes);
        preferredLocations = preferredLocations == null ? Set.of() : Set.copyOf(preferredLocations);
    }

    public boolean hasSkillNamed(String skillName) {
        if (skillName == null) {
            return false;
        }
        return skills.stream().anyMatch(skill -> skill.matchesName(skillName));
    }
}
