package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class SkillExtractor {

    private static final Map<Pattern, Skill> SKILL_PATTERNS = new LinkedHashMap<>();
    private static final Map<String, SkillCategory> KNOWN_TECH_SKILLS = new LinkedHashMap<>();
    private static final Map<String, Pattern> TERM_PATTERN_CACHE = new ConcurrentHashMap<>();

    private static final Pattern PRINCIPAL_PATTERN =
            Pattern.compile("\\b(principal|architect)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEAD_PATTERN = Pattern.compile("\\b(lead|head|staff)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENIOR_PATTERN = Pattern.compile("\\b(senior|sr)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUNIOR_PATTERN = Pattern.compile("\\b(junior|jr)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERN_PATTERN = Pattern.compile("\\b(intern|trainee)\\b", Pattern.CASE_INSENSITIVE);

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
        KNOWN_TECH_SKILLS.put("Ruby on Rails", SkillCategory.LANGUAGES_FRAMEWORKS);
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

        for (Map.Entry<String, SkillCategory> entry : KNOWN_TECH_SKILLS.entrySet()) {
            String skillName = entry.getKey();
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skillName.toLowerCase()) + "\\b");
            SKILL_PATTERNS.put(pattern, new Skill(skillName, entry.getValue(), 0));
        }

        SkillDictionary defaultDict = SkillDictionary.defaultDictionary();
        Set<String> defaultTerms = new HashSet<>(defaultDict.getAliasMap().keySet());
        defaultTerms.addAll(defaultDict.getAliasMap().values());
        for (String term : defaultTerms) {
            String lower = term.toLowerCase();
            TERM_PATTERN_CACHE.put(lower, Pattern.compile("\\b" + Pattern.quote(lower) + "\\b"));
        }
    }

    public static Set<Skill> extractSkills(String text) {
        return extractSkills(text, SkillDictionary.defaultDictionary());
    }

    public static Set<Skill> extractSkills(String text, SkillDictionary dictionary) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        SkillDictionary dict = dictionary != null ? dictionary : SkillDictionary.defaultDictionary();
        Set<Skill> extracted = new HashSet<>();
        String normalizedText = text.toLowerCase();

        for (Map.Entry<Pattern, Skill> entry : SKILL_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(normalizedText).find()) {
                String canonicalName = dict.resolveCanonical(entry.getValue().name());
                SkillCategory category = KNOWN_TECH_SKILLS.getOrDefault(
                        canonicalName, entry.getValue().category());
                extracted.add(
                        new Skill(canonicalName, category, entry.getValue().yearsExperience()));
            }
        }

        Set<String> searchTerms = new HashSet<>(dict.getAliasMap().keySet());
        searchTerms.addAll(dict.getAliasMap().values());

        for (String term : searchTerms) {
            Pattern pattern = getTermPattern(term);
            if (pattern.matcher(normalizedText).find()) {
                String canonicalName = dict.resolveCanonical(term);
                SkillCategory category = KNOWN_TECH_SKILLS.getOrDefault(canonicalName, SkillCategory.OTHER);
                extracted.add(new Skill(canonicalName, category, 0));
            }
        }

        return Set.copyOf(extracted);
    }

    private static Pattern getTermPattern(String term) {
        String lower = term.toLowerCase();
        return TERM_PATTERN_CACHE.computeIfAbsent(lower, t -> Pattern.compile("\\b" + Pattern.quote(t) + "\\b"));
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
