package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import java.util.Set;
import junit.framework.TestCase;

public class SkillExtractorDictionaryTest extends TestCase {

    public void testExtractSkillsWithAliasDictionary() {
        String text = "Looking for developer skilled in k8s, ror, postgres, and js.";
        SkillDictionary dict = SkillDictionary.defaultDictionary();

        Set<Skill> skills = SkillExtractor.extractSkills(text, dict);

        assertTrue(skills.stream().anyMatch(s -> s.name().equals("Kubernetes")));
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("Ruby on Rails")));
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("PostgreSQL")));
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("JavaScript")));
    }

    public void testExtractSkillsWithNullDictionaryFallback() {
        String text = "Looking for developer skilled in ror and k8s.";
        Set<Skill> skills = SkillExtractor.extractSkills(text, null);

        assertNotNull(skills);
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("Ruby on Rails")));
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("Kubernetes")));
    }

    public void testExtractSkillsSkillCategories() {
        String text = "Looking for developer skilled in ror, k8s, and postgres.";
        SkillDictionary dict = SkillDictionary.defaultDictionary();

        Set<Skill> skills = SkillExtractor.extractSkills(text, dict);

        Skill rorSkill = skills.stream()
                .filter(s -> s.name().equals("Ruby on Rails"))
                .findFirst()
                .orElse(null);
        assertNotNull(rorSkill);
        assertEquals(SkillCategory.LANGUAGES_FRAMEWORKS, rorSkill.category());

        Skill k8sSkill = skills.stream()
                .filter(s -> s.name().equals("Kubernetes"))
                .findFirst()
                .orElse(null);
        assertNotNull(k8sSkill);
        assertEquals(SkillCategory.DEVOPS_CLOUD, k8sSkill.category());

        Skill postgresSkill = skills.stream()
                .filter(s -> s.name().equals("PostgreSQL"))
                .findFirst()
                .orElse(null);
        assertNotNull(postgresSkill);
        assertEquals(SkillCategory.DATABASE, postgresSkill.category());
    }
}
