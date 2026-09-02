package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.*;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultAssessJobCompatibilityServiceTest extends TestCase {

    public void testServiceEvaluation() {
        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase useCase = new DefaultAssessJobCompatibilityService(engine);

        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Java Dev",
                "Company",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                null);

        MatchResult result = useCase.evaluate(job, profile, FilterConfiguration.defaultConfig());
        assertEquals(100.0, result.overallScore(), 0.01);
        assertEquals(MatchStatus.RECOMMENDED, result.status());

        List<MatchResult> listResult = useCase.evaluateAll(List.of(job), profile, FilterConfiguration.defaultConfig());
        assertEquals(1, listResult.size());
    }

    public void testConstructorNullCheck() {
        try {
            new DefaultAssessJobCompatibilityService(null);
            fail("Should throw NPE for null MatchingEngine");
        } catch (NullPointerException e) {
            assertEquals("matchingEngine cannot be null", e.getMessage());
        }
    }

    public void testEvaluateNullCheck() {
        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase service = new DefaultAssessJobCompatibilityService(engine);
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Java Dev",
                "Company",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                null);
        FilterConfiguration config = FilterConfiguration.defaultConfig();

        try {
            service.evaluate(null, profile, config);
            fail("Should throw NPE for null JobOpportunity");
        } catch (NullPointerException e) {
            assertEquals("job cannot be null", e.getMessage());
        }

        try {
            service.evaluate(job, null, config);
            fail("Should throw NPE for null CandidateProfile");
        } catch (NullPointerException e) {
            assertEquals("profile cannot be null", e.getMessage());
        }

        try {
            service.evaluate(job, profile, null);
            fail("Should throw NPE for null FilterConfiguration");
        } catch (NullPointerException e) {
            assertEquals("config cannot be null", e.getMessage());
        }
    }

    public void testEvaluateAllNullHandling() {
        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase service = new DefaultAssessJobCompatibilityService(engine);
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Java Dev",
                "Company",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                null);
        FilterConfiguration config = FilterConfiguration.defaultConfig();

        List<MatchResult> nullJobsResult = service.evaluateAll(null, profile, config);
        assertNotNull(nullJobsResult);
        assertTrue(nullJobsResult.isEmpty());

        List<JobOpportunity> jobsWithNull = Arrays.asList(job, null);
        List<MatchResult> listResult = service.evaluateAll(jobsWithNull, profile, config);
        assertEquals(1, listResult.size());
        assertEquals(100.0, listResult.get(0).overallScore(), 0.01);
    }
}
