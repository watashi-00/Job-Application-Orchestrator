package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.ResumePdfStorageRepository;
import java.util.Objects;
import java.util.Set;

public class DefaultIngestCandidateProfileService implements IngestCandidateProfileUseCase {

    private final CandidateProfileIngestor ingestor;
    private final CandidateProfileRepository repository;
    private final ResumePdfStorageRepository pdfRepository;

    public DefaultIngestCandidateProfileService(
            CandidateProfileIngestor ingestor, CandidateProfileRepository repository) {
        this(ingestor, repository, null);
    }

    public DefaultIngestCandidateProfileService(
            CandidateProfileIngestor ingestor,
            CandidateProfileRepository repository,
            ResumePdfStorageRepository pdfRepository) {
        this.ingestor = Objects.requireNonNull(ingestor, "ingestor cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.pdfRepository = pdfRepository;
    }

    @Override
    public CandidateProfile ingestFromPdf(byte[] pdfBytes, CandidatePreferences preferences) {
        Objects.requireNonNull(pdfBytes, "pdfBytes cannot be null");
        CandidatePreferences effectivePreferences =
                preferences != null ? preferences : new CandidatePreferences(null, Set.of(), Set.of(), Set.of());
        CandidateProfile profile = ingestor.ingest(pdfBytes, effectivePreferences);
        repository.save(profile);
        if (pdfRepository != null) {
            pdfRepository.savePdf(pdfBytes);
        }
        return profile;
    }
}
