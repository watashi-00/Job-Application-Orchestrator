package com.watashi.core.ports.out;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import java.util.List;
import java.util.Optional;

public interface RecruiterEmailRepository {
    void save(RecruiterEmailMessage message);

    List<RecruiterEmailMessage> findAll();

    Optional<RecruiterEmailMessage> findById(String id);
}
