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

    public void testSeniorityDistanceOneScoresSixty() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-sen-1",
                "Mid Java Dev",
                "CompanyA",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertEquals(60.0, result.breakdown().seniorityScore(), 0.01);
    }

    public void testSeniorityDistanceGreaterThanOneScoresZeroWithConflict() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-sen-2",
                "Junior Java Dev",
                "CompanyA",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.JUNIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertEquals(0.0, result.breakdown().seniorityScore(), 0.01);
        assertTrue(result.conflicts().stream().anyMatch(c -> c.startsWith("Seniority mismatch:")));
    }

    public void testSalaryDeficitScoresFortyWithConflict() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-sal-1",
                "Java Dev Low Pay",
                "CompanyA",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("6000"), new BigDecimal("8000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertEquals(40.0, result.breakdown().salaryScore(), 0.01);
        assertTrue(result.conflicts()
                .contains("Salary expectation conflict: Job salary offer is below candidate minimum requirement"));
    }

    public void testCurrencyMismatchAddsConflict() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        JobOpportunity job = new JobOpportunity(
                "job-sal-2",
                "Java Dev USD",
                "CompanyA",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "USD"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertTrue(result.conflicts()
                .contains("Currency mismatch: Job salary currency (USD) does not match candidate expectation (BRL)"));
    }

    public void testConditionalStatusVerdict() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        Skill python = new Skill("Python", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        Skill go = new Skill("Go", SkillCategory.LANGUAGES_FRAMEWORKS, 3);

        JobOpportunity job = new JobOpportunity(
                "job-cond",
                "Polyglot Dev",
                "CompanyA",
                "Desc",
                Set.of(java, python, go),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                new SalaryRange(new BigDecimal("12000"), new BigDecimal("14000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertTrue(result.overallScore() >= 50.0 && result.overallScore() < 75.0);
        assertEquals(MatchStatus.CONDITIONAL, result.status());
    }

    public void testLowScoreRejectedVerdict() {
        Skill cplusplus = new Skill("C++", SkillCategory.LANGUAGES_FRAMEWORKS, 3);

        JobOpportunity job = new JobOpportunity(
                "job-rej",
                "C++ Dev",
                "CompanyA",
                "Desc",
                Set.of(cplusplus),
                Set.of(),
                SeniorityLevel.JUNIOR,
                WorkMode.HYBRID,
                "Office",
                new SalaryRange(new BigDecimal("5000"), new BigDecimal("7000"), "BRL"),
                "http://example.com",
                JobStatus.DISCOVERED);

        MatchResult result = engine.evaluate(job, candidate, defaultConfig);
        assertTrue(result.overallScore() < 50.0);
        assertEquals(MatchStatus.REJECTED, result.status());
    }

    public void testNullParametersThrowException() {
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

        try {
            engine.evaluate(null, candidate, defaultConfig);
            fail("Should throw NPE for null job");
        } catch (NullPointerException e) {
            assertEquals("job cannot be null", e.getMessage());
        }

        try {
            engine.evaluate(job, null, defaultConfig);
            fail("Should throw NPE for null profile");
        } catch (NullPointerException e) {
            assertEquals("profile cannot be null", e.getMessage());
        }

        try {
            engine.evaluate(job, candidate, null);
            fail("Should throw NPE for null config");
        } catch (NullPointerException e) {
            assertEquals("config cannot be null", e.getMessage());
        }
    }

    public void testStrictRequiredSkillsMissingNamesSorted() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 3);
        Skill rust = new Skill("Rust", SkillCategory.LANGUAGES_FRAMEWORKS, 2);
        Skill go = new Skill("Go", SkillCategory.LANGUAGES_FRAMEWORKS, 2);

        JobOpportunity job = new JobOpportunity(
                "job-sorted",
                "Rust, Go & Java Dev",
                "CompanyB",
                "Desc",
                Set.of(java, rust, go),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "http://example.com",
                JobStatus.DISCOVERED);

        FilterConfiguration strictConfig = new FilterConfiguration(0.5, 0.2, 0.15, 0.15, 75.0, true);
        MatchResult result = engine.evaluate(job, candidate, strictConfig);

        assertTrue(result.conflicts().contains("Strict match failed: Missing mandatory required skill(s): Go, Rust"));
    }
}
