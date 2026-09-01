package com.watashi.core.domain.matching;

import com.watashi.core.domain.common.*;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

public class MatchResultTest extends TestCase {

    public void testFilterConfigurationDefaults() {
        FilterConfiguration config = FilterConfiguration.defaultConfig();
        assertEquals(0.50, config.techWeight(), 0.001);
        assertEquals(75.0, config.minimumScoreThreshold(), 0.001);
        assertFalse(config.strictRequiredSkills());
    }

    public void testMatchResultCreation() {
        ScoreBreakdown breakdown = new ScoreBreakdown(100.0, 100.0, 100.0, 100.0);
        MatchResult result = new MatchResult(
                "job-1",
                100.0,
                breakdown,
                Set.of(new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5)),
                Set.of(),
                Set.of(),
                List.of(),
                MatchStatus.RECOMMENDED);
        assertEquals(100.0, result.overallScore());
        assertEquals(MatchStatus.RECOMMENDED, result.status());
    }

    public void testMatchResultNullDefaultsAndDefensiveCopies() {
        MatchResult result = new MatchResult(
                "job-2", 50.0, new ScoreBreakdown(50.0, 50.0, 50.0, 50.0), null, null, null, null, null);
        assertEquals("job-2", result.jobId());
        assertEquals(50.0, result.overallScore(), 0.001);
        assertNotNull(result.matchedSkills());
        assertTrue(result.matchedSkills().isEmpty());
        assertNotNull(result.missingRequiredSkills());
        assertTrue(result.missingRequiredSkills().isEmpty());
        assertNotNull(result.missingOptionalSkills());
        assertTrue(result.missingOptionalSkills().isEmpty());
        assertNotNull(result.conflicts());
        assertTrue(result.conflicts().isEmpty());
        assertEquals(MatchStatus.REJECTED, result.status());
    }

    public void testMatchResultNullJobIdThrowsException() {
        try {
            new MatchResult(null, 100.0, null, Set.of(), Set.of(), Set.of(), List.of(), MatchStatus.RECOMMENDED);
            fail("Expected NullPointerException when jobId is null");
        } catch (NullPointerException e) {
            // expected
        }
    }
}
