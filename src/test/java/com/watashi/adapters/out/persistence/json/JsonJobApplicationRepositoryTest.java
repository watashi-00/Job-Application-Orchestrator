package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.out.JobApplicationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import junit.framework.TestCase;

public class JsonJobApplicationRepositoryTest extends TestCase {

    public void testPersistenceAndUpdates() throws Exception {
        Path tempFile = Files.createTempFile("applications-history-test", ".json");
        try {
            JobApplicationRepository repo1 = new JsonJobApplicationRepository(tempFile);
            assertTrue(repo1.loadHistory().isEmpty());

            repo1.updateJobStatus("job-1", JobStatus.APPLIED);
            repo1.updateJobStatus("job-2", JobStatus.INTERVIEWING);

            Map<String, JobStatus> history1 = repo1.loadHistory();
            assertEquals(2, history1.size());
            assertEquals(JobStatus.APPLIED, history1.get("job-1"));
            assertEquals(JobStatus.INTERVIEWING, history1.get("job-2"));

            // Re-instantiate from file to test load persistence
            JobApplicationRepository repo2 = new JsonJobApplicationRepository(tempFile);
            Map<String, JobStatus> history2 = repo2.loadHistory();
            assertEquals(2, history2.size());
            assertEquals(JobStatus.APPLIED, history2.get("job-1"));
            assertEquals(JobStatus.INTERVIEWING, history2.get("job-2"));

            // Test saveHistory bulk
            Map<String, JobStatus> bulkHistory = Map.of("job-3", JobStatus.OFFER, "job-4", JobStatus.REJECTED);
            repo2.saveHistory(bulkHistory);

            JobApplicationRepository repo3 = new JsonJobApplicationRepository(tempFile);
            assertEquals(2, repo3.loadHistory().size());
            assertEquals(JobStatus.OFFER, repo3.loadHistory().get("job-3"));
            assertEquals(JobStatus.REJECTED, repo3.loadHistory().get("job-4"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testNullChecks() throws Exception {
        Path tempFile = Files.createTempFile("applications-history-nulls", ".json");
        try {
            JobApplicationRepository repo = new JsonJobApplicationRepository(tempFile);

            try {
                repo.updateJobStatus(null, JobStatus.APPLIED);
                fail("Expected NPE");
            } catch (NullPointerException expected) {
            }

            try {
                repo.updateJobStatus("job-1", null);
                fail("Expected NPE");
            } catch (NullPointerException expected) {
            }

            try {
                repo.saveHistory(null);
                fail("Expected NPE");
            } catch (NullPointerException expected) {
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
