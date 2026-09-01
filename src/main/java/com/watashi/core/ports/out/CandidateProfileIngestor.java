package com.watashi.core.ports.out;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;

public interface CandidateProfileIngestor {
    CandidateProfile ingest(byte[] pdfBytes, CandidatePreferences preferences);
}
