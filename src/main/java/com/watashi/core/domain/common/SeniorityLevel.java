package com.watashi.core.domain.common;

public enum SeniorityLevel {
    INTERN(1),
    JUNIOR(2),
    MID(3),
    SENIOR(4),
    LEAD(5),
    PRINCIPAL(6);

    private final int level;

    SeniorityLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public int distanceTo(SeniorityLevel other) {
        if (other == null) {
            throw new IllegalArgumentException("Other SeniorityLevel cannot be null");
        }
        return Math.abs(this.level - other.level);
    }
}
