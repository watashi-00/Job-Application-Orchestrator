package com.watashi.core.domain.discovery;

import java.util.Map;
import junit.framework.TestCase;

public class SkillDictionaryTest extends TestCase {

    public void testResolveCanonical() {
        SkillDictionary dictionary = SkillDictionary.defaultDictionary();

        assertEquals("Kubernetes", dictionary.resolveCanonical("k8s"));
        assertEquals("JavaScript", dictionary.resolveCanonical("js"));
        assertEquals("Ruby on Rails", dictionary.resolveCanonical("ror"));
        assertEquals("PostgreSQL", dictionary.resolveCanonical("postgres"));
        assertEquals("UnknownTerm", dictionary.resolveCanonical("UnknownTerm"));
    }

    public void testCaseInsensitivityAndTrimming() {
        SkillDictionary dictionary = SkillDictionary.defaultDictionary();

        assertEquals("Kubernetes", dictionary.resolveCanonical("K8S"));
        assertEquals("JavaScript", dictionary.resolveCanonical(" JS "));
        assertEquals("TypeScript", dictionary.resolveCanonical("TypeScript"));
    }

    public void testAllDefaultAliases() {
        SkillDictionary dictionary = SkillDictionary.defaultDictionary();

        assertEquals("Kubernetes", dictionary.resolveCanonical("kube"));
        assertEquals("JavaScript", dictionary.resolveCanonical("es6"));
        assertEquals("JavaScript", dictionary.resolveCanonical("javascript"));
        assertEquals("TypeScript", dictionary.resolveCanonical("ts"));
        assertEquals("PostgreSQL", dictionary.resolveCanonical("postgresql"));
        assertEquals("PostgreSQL", dictionary.resolveCanonical("pgsql"));
        assertEquals("Ruby on Rails", dictionary.resolveCanonical("rails"));
        assertEquals("Ruby on Rails", dictionary.resolveCanonical("ruby on rails"));
        assertEquals("AWS", dictionary.resolveCanonical("aws"));
        assertEquals("AWS", dictionary.resolveCanonical("amazon web services"));
        assertEquals("React", dictionary.resolveCanonical("reactjs"));
        assertEquals("React", dictionary.resolveCanonical("react.js"));
        assertEquals("Vue", dictionary.resolveCanonical("vuejs"));
        assertEquals("Vue", dictionary.resolveCanonical("vue.js"));
        assertEquals("Node.js", dictionary.resolveCanonical("nodejs"));
        assertEquals("Node.js", dictionary.resolveCanonical("node.js"));
        assertEquals("Go", dictionary.resolveCanonical("golang"));
        assertEquals("Spring Boot", dictionary.resolveCanonical("spring"));
        assertEquals("Spring Boot", dictionary.resolveCanonical("springboot"));
    }

    public void testAddAlias() {
        SkillDictionary dictionary = new SkillDictionary();
        dictionary.addAlias("py", "Python");

        assertEquals("Python", dictionary.resolveCanonical("py"));
        assertEquals("Python", dictionary.resolveCanonical("PY"));
    }

    public void testGetAliasMap() {
        SkillDictionary dictionary = SkillDictionary.defaultDictionary();
        Map<String, String> aliasMap = dictionary.getAliasMap();

        assertNotNull(aliasMap);
        assertEquals("Kubernetes", aliasMap.get("k8s"));
        try {
            aliasMap.put("test", "test");
            fail("Alias map should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
}
