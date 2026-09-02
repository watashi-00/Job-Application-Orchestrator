package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.ResumePdfStorageRepository;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultIngestCandidateProfileServiceTest extends TestCase {

    private CandidateProfile mockProfile;
    private CandidateProfileIngestor mockIngestor;
    private CandidateProfileRepository mockRepo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockProfile = new CandidateProfile("c1", "Java Dev", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
        mockIngestor = (bytes, prefs) -> mockProfile;
        mockRepo = new CandidateProfileRepository() {
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
    }

    public void testIngestAndSaveProfile() {
        CandidatePreferences preferences = new CandidatePreferences(null, Set.of(), Set.of(), Set.of());
        IngestCandidateProfileUseCase useCase = new DefaultIngestCandidateProfileService(mockIngestor, mockRepo);
        CandidateProfile result = useCase.ingestFromPdf("Dummy".getBytes(), preferences);

        assertEquals(mockProfile, result);
        assertEquals(mockProfile, mockRepo.findDefault().orElse(null));
    }

    public void testIngestAndSavePdfToPdfRepository() {
        byte[][] savedBytes = new byte[1][];
        ResumePdfStorageRepository pdfRepo = new ResumePdfStorageRepository() {
            @Override
            public void savePdf(byte[] pdfBytes) {
                savedBytes[0] = pdfBytes;
            }

            @Override
            public Optional<byte[]> loadPdf() {
                return Optional.ofNullable(savedBytes[0]);
            }

            @Override
            public boolean exists() {
                return savedBytes[0] != null;
            }
        };

        IngestCandidateProfileUseCase useCase =
                new DefaultIngestCandidateProfileService(mockIngestor, mockRepo, pdfRepo);
        byte[] pdfInput = "PDF Content".getBytes();
        CandidateProfile result = useCase.ingestFromPdf(pdfInput, null);

        assertEquals(mockProfile, result);
        assertEquals(mockProfile, mockRepo.findDefault().orElse(null));
        assertEquals("PDF Content", new String(savedBytes[0]));
    }

    public void testConstructorNullIngestor() {
        try {
            new DefaultIngestCandidateProfileService(null, mockRepo);
            fail("Expected NullPointerException for null ingestor");
        } catch (NullPointerException expected) {
            assertEquals("ingestor cannot be null", expected.getMessage());
        }
    }

    public void testConstructorNullRepository() {
        try {
            new DefaultIngestCandidateProfileService(mockIngestor, null);
            fail("Expected NullPointerException for null repository");
        } catch (NullPointerException expected) {
            assertEquals("repository cannot be null", expected.getMessage());
        }
    }

    public void testIngestFromPdfNullPdfBytes() {
        IngestCandidateProfileUseCase useCase = new DefaultIngestCandidateProfileService(mockIngestor, mockRepo);
        try {
            useCase.ingestFromPdf(null, new CandidatePreferences(null, Set.of(), Set.of(), Set.of()));
            fail("Expected NullPointerException for null pdfBytes");
        } catch (NullPointerException expected) {
            assertEquals("pdfBytes cannot be null", expected.getMessage());
        }
    }

    public void testIngestFromPdfNullPreferencesDefaultsToEmpty() {
        final CandidatePreferences[] capturedPrefs = new CandidatePreferences[1];
        CandidateProfileIngestor capturingIngestor = (bytes, prefs) -> {
            capturedPrefs[0] = prefs;
            return mockProfile;
        };

        IngestCandidateProfileUseCase useCase = new DefaultIngestCandidateProfileService(capturingIngestor, mockRepo);
        CandidateProfile result = useCase.ingestFromPdf("Dummy".getBytes(), null);

        assertEquals(mockProfile, result);
        assertNotNull(capturedPrefs[0]);
        assertEquals(new CandidatePreferences(null, Set.of(), Set.of(), Set.of()), capturedPrefs[0]);
    }
}
