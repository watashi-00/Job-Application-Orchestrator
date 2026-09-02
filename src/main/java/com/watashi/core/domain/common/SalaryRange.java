package com.watashi.core.domain.common;

import java.math.BigDecimal;

public record SalaryRange(BigDecimal min, BigDecimal max, String currency) {

    public SalaryRange {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum salary cannot be negative");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum salary cannot be negative");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Minimum salary cannot be greater than maximum salary");
        }
    }

    public boolean coversMinimum(BigDecimal expectedMin) {
        if (expectedMin == null) {
            return true;
        }
        if (max == null) {
            return true;
        }
        return max.compareTo(expectedMin) >= 0;
    }
}
