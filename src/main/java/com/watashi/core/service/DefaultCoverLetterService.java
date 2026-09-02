package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.CoverLetterResult;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultCoverLetterService implements GenerateCoverLetterUseCase {

    @Override
    public CoverLetterResult generateCoverLetter(JobOpportunity job, CandidateProfile profile, MatchResult match) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(match, "match cannot be null");

        String coverLetter = buildCoverLetter(job, profile, match);
        String matchExplanation = buildMatchExplanation(job, profile, match);

        return new CoverLetterResult(job.id(), job.title(), job.company(), coverLetter, matchExplanation);
    }

    private String buildCoverLetter(JobOpportunity job, CandidateProfile profile, MatchResult match) {
        StringBuilder sb = new StringBuilder();

        String candidateRole =
                (profile.title() != null && !profile.title().isBlank()) ? profile.title() : "Software Professional";

        sb.append("# Cover Letter\n\n");
        sb.append("**Role:** ").append(job.title()).append("\n");
        sb.append("**Company:** ").append(job.company()).append("\n\n");

        sb.append("Dear Hiring Team at ").append(job.company()).append(",\n\n");

        sb.append("I am writing to express my strong interest in the ")
                .append(job.title())
                .append(" position. With my background as a ")
                .append(candidateRole)
                .append(", I am confident in my ability to bring immediate value to your team.\n\n");

        Set<Skill> matchedSkills = match.matchedSkills();
        if (matchedSkills != null && !matchedSkills.isEmpty()) {
            String skillsList = matchedSkills.stream().map(Skill::name).collect(Collectors.joining(", "));
            sb.append("My experience aligns closely with your core requirements, particularly in ")
                    .append(skillsList)
                    .append(".\n\n");
        }

        if (profile.summary() != null && !profile.summary().isBlank()) {
            sb.append(profile.summary()).append("\n\n");
        }

        sb.append(
                        "Thank you for considering my application. I look forward to the opportunity to discuss how my skills and background align with the needs of ")
                .append(job.company())
                .append(".\n\n");

        sb.append("Sincerely,\nCandidate");

        return sb.toString();
    }

    private String buildMatchExplanation(JobOpportunity job, CandidateProfile profile, MatchResult match) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Overall Match Score: %.1f%%\n", match.overallScore()));

        Set<Skill> matchedSkills = match.matchedSkills();
        if (matchedSkills != null && !matchedSkills.isEmpty()) {
            String skillsList = matchedSkills.stream().map(Skill::name).collect(Collectors.joining(", "));
            sb.append("Matched Skills: ").append(skillsList).append("\n");
        } else {
            sb.append("Matched Skills: None\n");
        }

        SeniorityLevel requiredSeniority = job.seniorityLevel();
        Set<SeniorityLevel> candidateSeniorities = profile.targetSeniorities();
        boolean seniorityAligned = requiredSeniority == null
                || (candidateSeniorities != null && candidateSeniorities.contains(requiredSeniority));

        sb.append("Seniority Alignment: ")
                .append(
                        seniorityAligned
                                ? "Aligned (" + requiredSeniority + ")"
                                : "Not Aligned (Required: " + requiredSeniority + ")")
                .append("\n");

        WorkMode jobWorkMode = job.workMode();
        Set<WorkMode> candidateWorkModes = profile.preferredWorkModes();
        boolean workModeAligned =
                jobWorkMode == null || (candidateWorkModes != null && candidateWorkModes.contains(jobWorkMode));

        sb.append("Work Mode Alignment: ")
                .append(
                        workModeAligned
                                ? "Aligned (" + jobWorkMode + ")"
                                : "Not Aligned (Required: " + jobWorkMode + ")");

        return sb.toString();
    }
}
