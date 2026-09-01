package com.watashi.core.ports.out;

import com.watashi.core.domain.matching.FilterConfiguration;

public interface FilterConfigRepository {
    FilterConfiguration load();

    void save(FilterConfiguration config);
}
