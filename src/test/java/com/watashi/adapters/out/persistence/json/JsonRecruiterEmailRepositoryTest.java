package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import junit.framework.TestCase;

public class JsonRecruiterEmailRepositoryTest extends TestCase {

    public void testEmptyRepositoryReturnsEmptyList() throws Exception {
        Path tempFile = Files.createTempFile("emails-inbox-empty-test", ".json");
        try {
            RecruiterEmailRepository repo = new JsonRecruiterEmailRepository(tempFile);
            assertTrue(repo.findAll().isEmpty());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testSaveFindAllFindByIdAndPersistence() throws Exception {
        Path tempFile = Files.createTempFile("emails-inbox-save-test", ".json");
        try {
            RecruiterEmailRepository repo1 = new JsonRecruiterEmailRepository(tempFile);
            RecruiterEmailMessage message = new RecruiterEmailMessage(
                    "msg-101",
                    "recruiter@acme.com",
                    "candidate@watashi.com",
                    "Interview Schedule",
                    "We would love to talk to you.",
                    Instant.now(),
                    "job-1",
                    "Acme Corp",
                    JobStatus.INTERVIEWING);

            repo1.save(message);

            List<RecruiterEmailMessage> list1 = repo1.findAll();
            assertEquals(1, list1.size());
            assertEquals("msg-101", list1.get(0).id());
            assertEquals("recruiter@acme.com", list1.get(0).sender());
            assertEquals("job-1", list1.get(0).matchedJobId());
            assertEquals(JobStatus.INTERVIEWING, list1.get(0).detectedStatus());

            Optional<RecruiterEmailMessage> found = repo1.findById("msg-101");
            assertTrue(found.isPresent());
            assertEquals("Acme Corp", found.get().matchedCompany());

            // Re-instantiate repo to verify file persistence
            RecruiterEmailRepository repo2 = new JsonRecruiterEmailRepository(tempFile);
            List<RecruiterEmailMessage> list2 = repo2.findAll();
            assertEquals(1, list2.size());
            assertEquals("msg-101", list2.get(0).id());
            assertEquals("recruiter@acme.com", list2.get(0).sender());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
