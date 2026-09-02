package com.watashi.core.ports.in;

import com.watashi.core.domain.application.ApplicationDispatchLog;
import java.util.List;

public interface DispatchJobApplicationUseCase {

    ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter);

    List<ApplicationDispatchLog> getDispatchLogs();

    void saveDomainCredential(String domain, String username, String token);
}
