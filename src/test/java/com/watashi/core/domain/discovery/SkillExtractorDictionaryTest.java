package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.Skill;
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
}
