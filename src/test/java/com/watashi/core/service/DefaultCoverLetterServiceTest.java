package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.domain.matching.CoverLetterResult;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchStatus;
import com.watashi.core.domain.matching.ScoreBreakdown;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultCoverLetterServiceTest extends TestCase {

    public void testGenerateCoverLetter() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        Skill spring = new Skill("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Senior Java Engineer",
                "Acme Corp",
                "Desc",
                Set.of(java, spring),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Backend Software Engineer",
                "Summary",
                Set.of(java, spring),
                Set.of(SeniorityLevel.SENIOR),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        MatchResult match = new MatchResult(
                "j1",
                100.0,
                new ScoreBreakdown(100, 100, 100, 100),
                Set.of(java, spring),
                Set.of(),
                Set.of(),
                List.of(),
                MatchStatus.RECOMMENDED);

        GenerateCoverLetterUseCase useCase = new DefaultCoverLetterService();
        CoverLetterResult result = useCase.generateCoverLetter(job, profile, match);

        assertNotNull(result);
        assertEquals("j1", result.jobId());
        assertEquals("Senior Java Engineer", result.jobTitle());
        assertEquals("Acme Corp", result.company());
        assertTrue(result.coverLetter().contains("Senior Java Engineer"));
        assertTrue(result.coverLetter().contains("Acme Corp"));
        assertTrue(result.coverLetter().contains("Java"));
        assertTrue(result.matchExplanation().contains("100.0%"));
    }

    public void testNullArguments() {
        GenerateCoverLetterUseCase useCase = new DefaultCoverLetterService();
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Senior Java Engineer",
                "Acme Corp",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Backend Software Engineer",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.SENIOR),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        MatchResult match = new MatchResult(
                "j1",
                100.0,
                new ScoreBreakdown(100, 100, 100, 100),
                Set.of(java),
                Set.of(),
                Set.of(),
                List.of(),
                MatchStatus.RECOMMENDED);

        try {
            useCase.generateCoverLetter(null, profile, match);
            fail("Should throw NPE for null job");
        } catch (NullPointerException e) {
            assertEquals("job cannot be null", e.getMessage());
        }

        try {
            useCase.generateCoverLetter(job, null, match);
            fail("Should throw NPE for null profile");
        } catch (NullPointerException e) {
            assertEquals("profile cannot be null", e.getMessage());
        }

        try {
            useCase.generateCoverLetter(job, profile, null);
            fail("Should throw NPE for null match");
        } catch (NullPointerException e) {
            assertEquals("match cannot be null", e.getMessage());
        }
    }
}
