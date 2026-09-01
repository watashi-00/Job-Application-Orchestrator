package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.util.Objects;
import java.util.Set;

public class DefaultIngestCandidateProfileService implements IngestCandidateProfileUseCase {

    private final CandidateProfileIngestor ingestor;
    private final CandidateProfileRepository repository;

    public DefaultIngestCandidateProfileService(
            CandidateProfileIngestor ingestor, CandidateProfileRepository repository) {
        this.ingestor = Objects.requireNonNull(ingestor, "ingestor cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public CandidateProfile ingestFromPdf(byte[] pdfBytes, CandidatePreferences preferences) {
        Objects.requireNonNull(pdfBytes, "pdfBytes cannot be null");
        CandidatePreferences effectivePreferences =
                preferences != null ? preferences : new CandidatePreferences(null, Set.of(), Set.of(), Set.of());
        CandidateProfile profile = ingestor.ingest(pdfBytes, effectivePreferences);
        repository.save(profile);
        return profile;
    }
}
