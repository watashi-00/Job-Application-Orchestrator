package com.watashi.core.domain.common;

import java.math.BigDecimal;

public record SalaryRange(BigDecimal min, BigDecimal max, String currency) {

    public SalaryRange {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
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
