package com.watashi.core.domain.job;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import java.util.Objects;
import java.util.Set;

public record JobOpportunity(
        String id,
        String title,
        String company,
        String description,
        Set<Skill> requiredSkills,
        Set<Skill> optionalSkills,
        SeniorityLevel seniorityLevel,
        WorkMode workMode,
        String location,
        SalaryRange salaryRange,
        String sourceUrl,
        JobStatus status) {

    public JobOpportunity {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(title, "title cannot be null");

        requiredSkills = requiredSkills == null ? Set.of() : Set.copyOf(requiredSkills);
        optionalSkills = optionalSkills == null ? Set.of() : Set.copyOf(optionalSkills);
        if (status == null) {
            status = JobStatus.DISCOVERED;
        }
    }

    public boolean hasSkillNamed(String skillName) {
        if (skillName == null) {
            return false;
        }
        return requiredSkills.stream().anyMatch(skill -> skill.matchesName(skillName))
                || optionalSkills.stream().anyMatch(skill -> skill.matchesName(skillName));
    }
}
