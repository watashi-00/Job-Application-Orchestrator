package com.watashi.core.ports.in;

import com.watashi.core.domain.common.SystemActivityLogEntry;
import java.util.List;

public interface GetSystemLogsUseCase {

    List<SystemActivityLogEntry> getUnifiedSystemLogs();
}
