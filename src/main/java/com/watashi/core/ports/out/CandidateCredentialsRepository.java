package com.watashi.core.ports.out;

import com.watashi.core.domain.application.CandidateCredential;
import java.util.Map;
import java.util.Optional;

public interface CandidateCredentialsRepository {

    Optional<CandidateCredential> findByDomain(String domain);

    void saveCredential(CandidateCredential credential);

    Map<String, CandidateCredential> findAll();
}
