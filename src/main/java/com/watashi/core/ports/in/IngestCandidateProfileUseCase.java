package com.watashi.core.ports.in;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;

public interface IngestCandidateProfileUseCase {
    CandidateProfile ingestFromPdf(byte[] pdfBytes, CandidatePreferences preferences);
}
