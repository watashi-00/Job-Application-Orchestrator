package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import java.util.Set;
import junit.framework.TestCase;

public class SkillExtractorTest extends TestCase {

    public void testExtractSkillsFromNullAndEmpty() {
        assertTrue(SkillExtractor.extractSkills(null).isEmpty());
        assertTrue(SkillExtractor.extractSkills("").isEmpty());
    }

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
        assertEquals(SeniorityLevel.PRINCIPAL, SkillExtractor.inferSeniority("Principal Architect"));

        assertEquals(SeniorityLevel.LEAD, SkillExtractor.inferSeniority("Staff Engineer"));
        assertEquals(SeniorityLevel.LEAD, SkillExtractor.inferSeniority("Head of Dev"));
        assertEquals(SeniorityLevel.LEAD, SkillExtractor.inferSeniority("Team Lead"));

        assertEquals(SeniorityLevel.SENIOR, SkillExtractor.inferSeniority("Sr. Developer"));
        assertEquals(SeniorityLevel.SENIOR, SkillExtractor.inferSeniority("Senior Java Dev"));

        assertEquals(SeniorityLevel.MID, SkillExtractor.inferSeniority("Software Engineer"));
        assertEquals(SeniorityLevel.MID, SkillExtractor.inferSeniority("Developer"));

        assertEquals(SeniorityLevel.JUNIOR, SkillExtractor.inferSeniority("Junior Developer"));
        assertEquals(SeniorityLevel.JUNIOR, SkillExtractor.inferSeniority("Jr Backend"));

        assertEquals(SeniorityLevel.INTERN, SkillExtractor.inferSeniority("Intern"));
        assertEquals(SeniorityLevel.INTERN, SkillExtractor.inferSeniority("Trainee"));
    }

    public void testInferSeniorityEdgeCases() {
        assertEquals(SeniorityLevel.JUNIOR, SkillExtractor.inferSeniority("Junior SRE"));
        assertEquals(SeniorityLevel.LEAD, SkillExtractor.inferSeniority("SRE Lead"));
    }
}
