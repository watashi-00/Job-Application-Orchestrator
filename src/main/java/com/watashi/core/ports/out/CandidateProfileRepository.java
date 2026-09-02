package com.watashi.core.ports.out;

import com.watashi.core.domain.candidate.CandidateProfile;
import java.util.Optional;

public interface CandidateProfileRepository {
    Optional<CandidateProfile> findDefault();

    void save(CandidateProfile profile);
}
