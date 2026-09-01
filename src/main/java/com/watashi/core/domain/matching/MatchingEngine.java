package com.watashi.core.domain.matching;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MatchingEngine {

    public MatchResult evaluate(JobOpportunity job, CandidateProfile profile, FilterConfiguration config) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        List<String> conflicts = new ArrayList<>();

        // 1. Technical Skills Score
        Set<Skill> matchedSkills = new HashSet<>();
        Set<Skill> missingRequired = new HashSet<>();
        Set<Skill> missingOptional = new HashSet<>();

        for (Skill req : job.requiredSkills()) {
            if (profile.hasSkillNamed(req.name())) {
                matchedSkills.add(req);
            } else {
                missingRequired.add(req);
            }
        }

        for (Skill opt : job.optionalSkills()) {
            if (profile.hasSkillNamed(opt.name())) {
                matchedSkills.add(opt);
            } else {
                missingOptional.add(opt);
            }
        }

        double reqRatio;
        if (job.requiredSkills().isEmpty()) {
            reqRatio = matchedSkills.isEmpty() ? 0.20 : 1.0;
        } else {
            reqRatio = (double) (job.requiredSkills().size() - missingRequired.size())
                    / job.requiredSkills().size();
        }

        double optRatio = job.optionalSkills().isEmpty()
                ? 1.0
                : (double) (job.optionalSkills().size() - missingOptional.size())
                        / job.optionalSkills().size();

        double techScore = (reqRatio * 85.0) + (optRatio * 15.0);

        boolean strictFailed = false;
        if (config.strictRequiredSkills() && !missingRequired.isEmpty()) {
            strictFailed = true;
            String missingNames =
                    missingRequired.stream().map(Skill::name).sorted().collect(Collectors.joining(", "));
            conflicts.add("Strict match failed: Missing mandatory required skill(s): " + missingNames);
        }

        // Title / Role Relevance Check
        boolean roleMismatch = isRoleUnrelated(profile.title(), job.title());
        if (roleMismatch) {
            conflicts.add("Role title mismatch: Job title '" + job.title()
                    + "' does not match candidate target domain (" + profile.title() + ")");
        }

        // 2. Seniority Score
        double seniorityScore = 100.0;
        if (job.seniorityLevel() != null
                && profile.targetSeniorities() != null
                && !profile.targetSeniorities().isEmpty()) {
            if (profile.targetSeniorities().contains(job.seniorityLevel())) {
                seniorityScore = 100.0;
            } else {
                int minDistance = profile.targetSeniorities().stream()
                        .filter(Objects::nonNull)
                        .mapToInt(s -> s.distanceTo(job.seniorityLevel()))
                        .min()
                        .orElse(99);
                if (minDistance == 1) {
                    seniorityScore = 60.0;
                } else {
                    seniorityScore = 0.0;
                    conflicts.add("Seniority mismatch: Job requires "
                            + job.seniorityLevel()
                            + " but candidate target is "
                            + profile.targetSeniorities());
                }
            }
        }

        // 3. Work Mode Score
        double workModeScore = 100.0;
        if (job.workMode() != null
                && profile.preferredWorkModes() != null
                && !profile.preferredWorkModes().isEmpty()) {
            if (profile.preferredWorkModes().contains(job.workMode())) {
                workModeScore = 100.0;
            } else if (profile.preferredWorkModes().equals(Set.of(WorkMode.REMOTE))
                    && job.workMode() == WorkMode.ONSITE) {
                workModeScore = 0.0;
                conflicts.add("Work mode conflict: Job requires ONSITE but candidate only accepts REMOTE");
            } else {
                workModeScore = 50.0;
            }
        }

        // 4. Salary Score
        double salaryScore = 100.0;
        if (job.salaryRange() != null && profile.desiredSalary() != null) {
            if (job.salaryRange().currency() != null
                    && profile.desiredSalary().currency() != null
                    && !job.salaryRange()
                            .currency()
                            .equals(profile.desiredSalary().currency())) {
                conflicts.add("Currency mismatch: Job salary currency ("
                        + job.salaryRange().currency()
                        + ") does not match candidate expectation ("
                        + profile.desiredSalary().currency()
                        + ")");
            } else if (!job.salaryRange().coversMinimum(profile.desiredSalary().min())) {
                salaryScore = 40.0;
                conflicts.add("Salary expectation conflict: Job salary offer is below candidate minimum requirement");
            }
        }

        // Overall Weighted Calculation
        double overallScore = (techScore * config.techWeight())
                + (seniorityScore * config.seniorityWeight())
                + (workModeScore * config.workModeWeight())
                + (salaryScore * config.salaryWeight());

        if (roleMismatch) {
            overallScore *= 0.50; // Apply 50% penalty for unrelated domain roles
        }

        MatchStatus status;
        if (strictFailed || roleMismatch || overallScore < 50.0) {
            status = MatchStatus.REJECTED;
        } else if (overallScore >= config.minimumScoreThreshold()) {
            status = MatchStatus.RECOMMENDED;
        } else {
            status = MatchStatus.CONDITIONAL;
        }

        ScoreBreakdown breakdown = new ScoreBreakdown(techScore, seniorityScore, workModeScore, salaryScore);
        return new MatchResult(
                job.id(), overallScore, breakdown, matchedSkills, missingRequired, missingOptional, conflicts, status);
    }

    private boolean isRoleUnrelated(String candidateTitle, String jobTitle) {
        if (candidateTitle == null || jobTitle == null) return false;
        String candLower = candidateTitle.toLowerCase();
        String jobLower = jobTitle.toLowerCase();

        boolean candIsTech = candLower.contains("engineer")
                || candLower.contains("developer")
                || candLower.contains("backend")
                || candLower.contains("frontend")
                || candLower.contains("fullstack")
                || candLower.contains("software")
                || candLower.contains("qa");

        if (candIsTech) {
            boolean jobIsNonTech = jobLower.contains("marketing")
                    || jobLower.contains("writer")
                    || jobLower.contains("sales")
                    || jobLower.contains("copywriter")
                    || jobLower.contains("recruiter")
                    || jobLower.contains("accounting")
                    || jobLower.contains("hr")
                    || jobLower.contains("content reviewer")
                    || jobLower.contains("collection");

            boolean jobHasTechKeyword = jobLower.contains("engineer")
                    || jobLower.contains("developer")
                    || jobLower.contains("software")
                    || jobLower.contains("qa")
                    || jobLower.contains("tech lead")
                    || jobLower.contains("backend")
                    || jobLower.contains("frontend");

            return jobIsNonTech && !jobHasTechKeyword;
        }
        return false;
    }
}
