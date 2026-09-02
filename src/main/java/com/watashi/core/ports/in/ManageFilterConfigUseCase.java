package com.watashi.core.ports.in;

import com.watashi.core.domain.matching.FilterConfiguration;

public interface ManageFilterConfigUseCase {
    FilterConfiguration getConfig();

    void updateConfig(FilterConfiguration config);
}
