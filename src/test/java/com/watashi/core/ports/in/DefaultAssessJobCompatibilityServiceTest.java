package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.*;
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
}
