package com.watashi.core.service;

import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.domain.common.SystemActivityLogEntry;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.in.GetSystemLogsUseCase;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DefaultGetSystemLogsService implements GetSystemLogsUseCase {

    private final DispatchJobApplicationUseCase dispatchUseCase;
    private final SyncRecruiterInboxUseCase inboxUseCase;

    public DefaultGetSystemLogsService(
            DispatchJobApplicationUseCase dispatchUseCase, SyncRecruiterInboxUseCase inboxUseCase) {
        this.dispatchUseCase = dispatchUseCase;
        this.inboxUseCase = inboxUseCase;
    }

    @Override
    public List<SystemActivityLogEntry> getUnifiedSystemLogs() {
        List<SystemActivityLogEntry> logs = new ArrayList<>();

        if (dispatchUseCase != null) {
            List<ApplicationDispatchLog> dispatchLogs = dispatchUseCase.getDispatchLogs();
            if (dispatchLogs != null) {
                for (ApplicationDispatchLog dispatch : dispatchLogs) {
                    if (dispatch == null) {
                        continue;
                    }
                    String id = "dispatch-" + dispatch.jobId() + "-" + dispatch.timestamp();
                    String title = dispatch.jobTitle() + " at " + dispatch.company();
                    logs.add(new SystemActivityLogEntry(
                            id,
                            "APPLICATION_DISPATCH",
                            title,
                            dispatch.message(),
                            dispatch.status(),
                            dispatch.timestamp(),
                            null));
                }
            }
        }

        if (inboxUseCase != null) {
            List<RecruiterEmailMessage> emails = inboxUseCase.getAllReceivedEmails();
            if (emails != null) {
                for (RecruiterEmailMessage email : emails) {
                    if (email == null) {
                        continue;
                    }
                    String id = "email-" + email.id();
                    String statusStr = email.detectedStatus() != null
                            ? email.detectedStatus().name()
                            : "RECEIVED";
                    String timestampStr = email.receivedAt() != null
                            ? email.receivedAt().toString()
                            : Instant.now().toString();

                    String gmailUrl = null;
                    if (email.sender() != null && !email.sender().isBlank()) {
                        gmailUrl = "https://mail.google.com/mail/u/0/#search/from%3A"
                                + URLEncoder.encode(email.sender(), StandardCharsets.UTF_8);
                    }

                    logs.add(new SystemActivityLogEntry(
                            id,
                            "RECRUITER_EMAIL",
                            email.subject(),
                            email.bodyText(),
                            statusStr,
                            timestampStr,
                            gmailUrl));
                }
            }
        }

        logs.sort(Comparator.comparing(SystemActivityLogEntry::timestamp).reversed());
        return List.copyOf(logs);
    }
}
