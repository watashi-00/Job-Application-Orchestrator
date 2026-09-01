package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.*;
import java.util.*;
import java.util.regex.Pattern;

public class SkillExtractor {

    private static final Map<String, SkillCategory> KNOWN_TECH_SKILLS = new LinkedHashMap<>();

    static {
        KNOWN_TECH_SKILLS.put("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Java", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Python", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("TypeScript", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("JavaScript", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("React", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Angular", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Vue", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Node.js", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Go", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Rust", SkillCategory.LANGUAGES_FRAMEWORKS);
        KNOWN_TECH_SKILLS.put("Docker", SkillCategory.DEVOPS_CLOUD);
        KNOWN_TECH_SKILLS.put("Kubernetes", SkillCategory.DEVOPS_CLOUD);
        KNOWN_TECH_SKILLS.put("AWS", SkillCategory.DEVOPS_CLOUD);
        KNOWN_TECH_SKILLS.put("PostgreSQL", SkillCategory.DATABASE);
        KNOWN_TECH_SKILLS.put("MySQL", SkillCategory.DATABASE);
        KNOWN_TECH_SKILLS.put("MongoDB", SkillCategory.DATABASE);
        KNOWN_TECH_SKILLS.put("Redis", SkillCategory.DATABASE);
        KNOWN_TECH_SKILLS.put("Microservices", SkillCategory.ARCHITECTURE_DESIGN);
        KNOWN_TECH_SKILLS.put("REST", SkillCategory.ARCHITECTURE_DESIGN);
        KNOWN_TECH_SKILLS.put("GraphQL", SkillCategory.ARCHITECTURE_DESIGN);
    }

    public static Set<Skill> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<Skill> extracted = new HashSet<>();
        String normalizedText = text.toLowerCase();

        for (Map.Entry<String, SkillCategory> entry : KNOWN_TECH_SKILLS.entrySet()) {
            String skillName = entry.getKey();
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skillName.toLowerCase()) + "\\b");
            if (pattern.matcher(normalizedText).find()) {
                extracted.add(new Skill(skillName, entry.getValue(), 0));
            }
        }
        return Set.copyOf(extracted);
    }

    public static SeniorityLevel inferSeniority(String title) {
        if (title == null || title.isBlank()) {
            return SeniorityLevel.MID;
        }

        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("principal") || lowerTitle.contains("architect")) {
            return SeniorityLevel.PRINCIPAL;
        } else if (lowerTitle.contains("lead") || lowerTitle.contains("head") || lowerTitle.contains("staff")) {
            return SeniorityLevel.LEAD;
        } else if (lowerTitle.contains("senior") || lowerTitle.contains("sr")) {
            return SeniorityLevel.SENIOR;
        } else if (lowerTitle.contains("junior") || lowerTitle.contains("jr")) {
            return SeniorityLevel.JUNIOR;
        } else if (lowerTitle.contains("intern") || lowerTitle.contains("trainee")) {
            return SeniorityLevel.INTERN;
        }

        return SeniorityLevel.MID;
    }
}
