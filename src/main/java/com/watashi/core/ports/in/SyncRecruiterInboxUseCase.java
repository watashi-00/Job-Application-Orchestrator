package com.watashi.core.ports.in;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import java.util.List;

public interface SyncRecruiterInboxUseCase {
    RecruiterEmailMessage receiveIncomingEmail(String sender, String recipient, String subject, String bodyText);

    List<RecruiterEmailMessage> getAllReceivedEmails();
}
