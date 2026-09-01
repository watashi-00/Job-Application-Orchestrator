package com.watashi.adapters.out.persistence.json;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.out.JobRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

public class JsonJobRepositoryTest extends TestCase {

    public void testJobPersistence() throws Exception {
        Path tempFile = Files.createTempFile("jobs-test", ".json");
        try {
            JobRepository repo1 = new JsonJobRepository(tempFile);
            JobOpportunity job = new JobOpportunity(
                    "j1",
                    "Java Dev",
                    "Acme",
                    "Desc",
                    java.util.Set.of(),
                    java.util.Set.of(),
                    SeniorityLevel.SENIOR,
                    WorkMode.REMOTE,
                    "Remote",
                    null,
                    "url",
                    JobStatus.DISCOVERED);

            repo1.save(job);
            assertEquals(1, repo1.findAll().size());

            // Re-instantiate from same file to test persistence load
            JobRepository repo2 = new JsonJobRepository(tempFile);
            assertEquals(1, repo2.findAll().size());
            assertEquals("j1", repo2.findById("j1").orElseThrow().id());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
