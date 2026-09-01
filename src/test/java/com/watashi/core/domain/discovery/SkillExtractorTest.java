package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.*;
import java.util.Set;
import junit.framework.TestCase;

public class SkillExtractorTest extends TestCase {

    public void testExtractSkillsFromText() {
        String text =
                "We are looking for a Senior Java Developer with Spring Boot, Docker, and PostgreSQL" + " experience.";
        Set<Skill> skills = SkillExtractor.extractSkills(text);

        assertTrue(skills.stream().anyMatch(s -> s.matchesName("Java")));
        assertTrue(skills.stream().anyMatch(s -> s.matchesName("Spring Boot")));
        assertTrue(skills.stream().anyMatch(s -> s.matchesName("Docker")));
        assertTrue(skills.stream().anyMatch(s -> s.matchesName("PostgreSQL")));
    }

    public void testInferSeniorityFromTitle() {
        assertEquals(SeniorityLevel.SENIOR, SkillExtractor.inferSeniority("Senior Backend Engineer"));
        assertEquals(SeniorityLevel.JUNIOR, SkillExtractor.inferSeniority("Junior Java Developer"));
        assertEquals(SeniorityLevel.MID, SkillExtractor.inferSeniority("Software Developer"));
    }
}
