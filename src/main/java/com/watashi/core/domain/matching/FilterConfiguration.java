package com.watashi.core.domain.matching;

public record FilterConfiguration(
        double techWeight,
        double seniorityWeight,
        double workModeWeight,
        double salaryWeight,
        double minimumScoreThreshold,
        boolean strictRequiredSkills) {
    public static FilterConfiguration defaultConfig() {
        return new FilterConfiguration(0.50, 0.20, 0.15, 0.15, 75.0, false);
    }
}
