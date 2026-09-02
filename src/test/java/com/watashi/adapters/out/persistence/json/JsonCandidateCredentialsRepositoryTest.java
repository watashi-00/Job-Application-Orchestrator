package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.application.CandidateCredential;
import com.watashi.core.ports.out.CandidateCredentialsRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import junit.framework.TestCase;

public class JsonCandidateCredentialsRepositoryTest extends TestCase {

    public void testCredentialPersistence() throws Exception {
        Path tempFile = Files.createTempFile("credentials-test", ".json");
        try {
            CandidateCredentialsRepository repo = new JsonCandidateCredentialsRepository(tempFile);
            CandidateCredential credential =
                    new CandidateCredential("lever.co", "rodrigo", "token123", LocalDateTime.now());

            repo.saveCredential(credential);

            CandidateCredentialsRepository repo2 = new JsonCandidateCredentialsRepository(tempFile);
            assertTrue(repo2.findByDomain("lever.co").isPresent());
            assertEquals("token123", repo2.findByDomain("lever.co").get().tokenOrSessionCookie());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testFindAllAndNonExistentDomain() throws Exception {
        Path tempFile = Files.createTempFile("credentials-test-all", ".json");
        try {
            CandidateCredentialsRepository repo = new JsonCandidateCredentialsRepository(tempFile);
            assertFalse(repo.findByDomain("greenhouse.io").isPresent());
            assertFalse(repo.findByDomain(null).isPresent());

            CandidateCredential cred1 = new CandidateCredential("greenhouse.io", "user1", "tok1", LocalDateTime.now());
            CandidateCredential cred2 = new CandidateCredential("workable.com", "user2", "tok2", LocalDateTime.now());

            repo.saveCredential(cred1);
            repo.saveCredential(cred2);

            Map<String, CandidateCredential> all = repo.findAll();
            assertEquals(2, all.size());
            assertEquals("user1", all.get("greenhouse.io").username());
            assertEquals("user2", all.get("workable.com").username());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
