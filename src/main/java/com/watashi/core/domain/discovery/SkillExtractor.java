package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SkillExtractor {

    private static final Map<Pattern, Skill> SKILL_PATTERNS = new LinkedHashMap<>();

    private static final Pattern PRINCIPAL_PATTERN =
            Pattern.compile("\\b(principal|architect)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAD_PATTERN = Pattern.compile("\\b(lead|head|staff)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENIOR_PATTERN = Pattern.compile("\\b(senior|sr)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUNIOR_PATTERN = Pattern.compile("\\b(junior|jr)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERN_PATTERN = Pattern.compile("\\b(intern|trainee)\\b", Pattern.CASE_INSENSITIVE);

    static {
        Map<String, SkillCategory> knownTechSkills = new LinkedHashMap<>();
        knownTechSkills.put("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Java", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Python", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("TypeScript", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("JavaScript", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("React", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Angular", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Vue", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Node.js", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Go", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Rust", SkillCategory.LANGUAGES_FRAMEWORKS);
        knownTechSkills.put("Docker", SkillCategory.DEVOPS_CLOUD);
        knownTechSkills.put("Kubernetes", SkillCategory.DEVOPS_CLOUD);
        knownTechSkills.put("AWS", SkillCategory.DEVOPS_CLOUD);
        knownTechSkills.put("PostgreSQL", SkillCategory.DATABASE);
        knownTechSkills.put("MySQL", SkillCategory.DATABASE);
        knownTechSkills.put("MongoDB", SkillCategory.DATABASE);
        knownTechSkills.put("Redis", SkillCategory.DATABASE);
        knownTechSkills.put("Microservices", SkillCategory.ARCHITECTURE_DESIGN);
        knownTechSkills.put("REST", SkillCategory.ARCHITECTURE_DESIGN);
        knownTechSkills.put("GraphQL", SkillCategory.ARCHITECTURE_DESIGN);

        for (Map.Entry<String, SkillCategory> entry : knownTechSkills.entrySet()) {
            String skillName = entry.getKey();
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skillName.toLowerCase()) + "\\b");
            SKILL_PATTERNS.put(pattern, new Skill(skillName, entry.getValue(), 0));
        }
    }

    public static Set<Skill> extractSkills(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<Skill> extracted = new HashSet<>();
        String normalizedText = text.toLowerCase();

        for (Map.Entry<Pattern, Skill> entry : SKILL_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(normalizedText).find()) {
                extracted.add(entry.getValue());
            }
        }
        return Set.copyOf(extracted);
    }

    public static SeniorityLevel inferSeniority(String title) {
        if (title == null || title.isBlank()) {
            return SeniorityLevel.MID;
        }

        if (PRINCIPAL_PATTERN.matcher(title).find()) {
            return SeniorityLevel.PRINCIPAL;
        } else if (LEAD_PATTERN.matcher(title).find()) {
            return SeniorityLevel.LEAD;
        } else if (SENIOR_PATTERN.matcher(title).find()) {
            return SeniorityLevel.SENIOR;
        } else if (JUNIOR_PATTERN.matcher(title).find()) {
            return SeniorityLevel.JUNIOR;
        } else if (INTERN_PATTERN.matcher(title).find()) {
            return SeniorityLevel.INTERN;
        }

        return SeniorityLevel.MID;
    }
}
