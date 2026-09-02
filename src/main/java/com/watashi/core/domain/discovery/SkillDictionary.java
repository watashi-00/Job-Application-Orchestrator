package com.watashi.core.domain.discovery;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillDictionary {

    private final Map<String, String> aliasToCanonicalMap;

    public SkillDictionary() {
        this.aliasToCanonicalMap = new ConcurrentHashMap<>();
    }

    public SkillDictionary(Map<String, String> initialAliases) {
        this();
        if (initialAliases != null) {
            initialAliases.forEach(this::addAlias);
        }
    }

    public static SkillDictionary defaultDictionary() {
        SkillDictionary dictionary = new SkillDictionary();

        dictionary.addAlias("k8s", "Kubernetes");
        dictionary.addAlias("kube", "Kubernetes");

        dictionary.addAlias("js", "JavaScript");
        dictionary.addAlias("es6", "JavaScript");
        dictionary.addAlias("javascript", "JavaScript");

        dictionary.addAlias("ts", "TypeScript");
        dictionary.addAlias("typescript", "TypeScript");

        dictionary.addAlias("postgres", "PostgreSQL");
        dictionary.addAlias("postgresql", "PostgreSQL");
        dictionary.addAlias("pgsql", "PostgreSQL");

        dictionary.addAlias("ror", "Ruby on Rails");
        dictionary.addAlias("rails", "Ruby on Rails");
        dictionary.addAlias("ruby on rails", "Ruby on Rails");

        dictionary.addAlias("aws", "AWS");
        dictionary.addAlias("amazon web services", "AWS");

        dictionary.addAlias("reactjs", "React");
        dictionary.addAlias("react.js", "React");

        dictionary.addAlias("vuejs", "Vue");
        dictionary.addAlias("vue.js", "Vue");

        dictionary.addAlias("nodejs", "Node.js");
        dictionary.addAlias("node.js", "Node.js");

        dictionary.addAlias("golang", "Go");

        dictionary.addAlias("spring", "Spring Boot");
        dictionary.addAlias("springboot", "Spring Boot");

        return dictionary;
    }

    public void addAlias(String alias, String canonical) {
        if (alias != null && canonical != null) {
            aliasToCanonicalMap.put(alias.toLowerCase().trim(), canonical);
        }
    }

    public String resolveCanonical(String term) {
        if (term == null) {
            return null;
        }
        String normalizedTerm = term.toLowerCase().trim();
        String canonical = aliasToCanonicalMap.get(normalizedTerm);
        return canonical != null ? canonical : term;
    }

    public Map<String, String> getAliasMap() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(aliasToCanonicalMap));
    }
}
