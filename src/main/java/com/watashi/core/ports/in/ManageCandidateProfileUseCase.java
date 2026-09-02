package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidateProfile;
import java.util.Optional;

public interface ManageCandidateProfileUseCase {
    Optional<CandidateProfile> getProfile();

    void updateProfile(CandidateProfile profile);
}
