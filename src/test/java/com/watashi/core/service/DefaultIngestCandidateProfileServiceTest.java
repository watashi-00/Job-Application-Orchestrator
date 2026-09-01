package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultIngestCandidateProfileServiceTest extends TestCase {

    public void testIngestAndSaveProfile() {
        CandidateProfile mockProfile =
                new CandidateProfile("c1", "Java Dev", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());

        CandidateProfileIngestor mockIngestor = (bytes, prefs) -> mockProfile;
        CandidateProfileRepository mockRepo = new CandidateProfileRepository() {
            private CandidateProfile saved;

            @Override
            public Optional<CandidateProfile> findDefault() {
                return Optional.ofNullable(saved);
            }

            @Override
            public void save(CandidateProfile profile) {
                this.saved = profile;
            }
        };

        IngestCandidateProfileUseCase useCase = new DefaultIngestCandidateProfileService(mockIngestor, mockRepo);
        CandidateProfile result = useCase.ingestFromPdf("Dummy".getBytes(), null);

        assertEquals(mockProfile, result);
        assertEquals(mockProfile, mockRepo.findDefault().orElse(null));
    }
}
