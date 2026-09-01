package com.watashi.core.domain.matching;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import java.math.BigDecimal;
import java.util.Set;
import junit.framework.TestCase;

public class MatchingEngineTest extends TestCase {

    private MatchingEngine engine;
    private CandidateProfile candidate;
    private FilterConfiguration defaultConfig;

    protected void setUp() throws Exception {
        engine = new MatchingEngine();
        defaultConfig = FilterConfiguration.defaultConfig();

        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        Skill spring = new Skill("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS, 4);

        candidate = new CandidateProfile(
                "cand-1",
                "Backend Engineer",
                "Java Dev",
                Set.of(java, spring),
                Set.of(SeniorityLevel.SENIOR),
                Set.of(WorkMode.REMOTE),
                new SalaryRange(new BigDecimal("10000"), new BigDecimal("15000"), "BRL"),
                Set.of("Remote"));
    }

    public void testPerfectMatchScoresHigh() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-1",
                "Senior Java Dev",
                "CompanyA",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertEquals(100.0, result.overallScore(), 0.01);
        assertEquals(MatchStatus.RECOMMENDED, result.status());
        assertTrue(result.conflicts().isEmpty());
    }

    public void testStrictRequiredSkillsRejection() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        Skill rust = new Skill("Rust", SkillCategory.LANGUAGES_FRAMEWORKS, 2);

        JobOpportunity job = new JobOpportunity(
                "job-2",
                "Rust & Java Dev",
                "CompanyB",
                "Desc",
                Set.of(java, rust),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "http://example.com",
                JobStatus.DISCOVERED);

        FilterConfiguration strictConfig = new FilterConfiguration(0.5, 0.2, 0.15, 0.15, 75.0, true);
        MatchResult result = engine.evaluate(job, candidate, strictConfig);

        assertEquals(MatchStatus.REJECTED, result.status());
        assertFalse(result.conflicts().isEmpty());
    }

    public void testWorkModeConflictRemoteOnlyVsOnsite() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-3",
                "Java Dev Onsite",
                "CompanyC",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.ONSITE,
                "Office",
                null,
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertTrue(result.conflicts().stream().anyMatch(c -> c.contains("ONSITE")));
    }
}
