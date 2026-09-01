package com.watashi.core.domain.common;

public record Skill(String name, SkillCategory category, int yearsExperience) {

    public Skill {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name cannot be null or empty");
        }
        name = name.trim();
        if (category == null) {
            category = SkillCategory.OTHER;
        }
        if (yearsExperience < 0) {
            throw new IllegalArgumentException("Years of experience cannot be negative");
        }
    }

    public String nameLower() {
        return name.toLowerCase();
    }

    public boolean matchesName(String otherName) {
        if (otherName == null) {
            return false;
        }
        return nameLower().equalsIgnoreCase(otherName.trim());
    }
}
