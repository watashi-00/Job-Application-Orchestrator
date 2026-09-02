package com.watashi.core.service;

import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.domain.application.CandidateCredential;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.out.CandidateCredentialsRepository;
import com.watashi.core.ports.out.JobApplicationRepository;
import com.watashi.core.ports.out.JobRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultDispatchJobApplicationServiceTest extends TestCase {

    private MemoryJobRepository jobRepo;
    private MemoryJobApplicationRepository appRepo;
    private MemoryCandidateCredentialsRepository credentialsRepo;
    private DispatchJobApplicationUseCase service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jobRepo = new MemoryJobRepository();
        appRepo = new MemoryJobApplicationRepository();
        credentialsRepo = new MemoryCandidateCredentialsRepository();
        service = new DefaultDispatchJobApplicationService(
                jobRepo, appRepo, credentialsRepo, Set.of("lever.co", "workday.com"));
    }

    public void testSuccessfulDispatchGeneratingDispatchedSuccessLog() {
        JobOpportunity job = new JobOpportunity(
                "job-1",
                "Software Engineer",
                "Acme Corp",
                "Description",
                Set.of(),
                Set.of(),
                null,
                null,
                "Remote",
                null,
                "https://example.com/jobs/1",
                JobStatus.DISCOVERED);
        jobRepo.save(job);

        ApplicationDispatchLog log = service.dispatchApplication("job-1", false);

        assertNotNull(log);
        assertEquals("job-1", log.jobId());
        assertEquals("Software Engineer", log.jobTitle());
        assertEquals("Acme Corp", log.company());
        assertEquals("DISPATCHED_SUCCESS", log.status());
        assertFalse(log.requiresHumanAssistance());
        assertEquals("example.com", log.domain());
        assertEquals(JobStatus.APPLIED, appRepo.loadHistory().get("job-1"));
        assertEquals(JobStatus.APPLIED, jobRepo.findById("job-1").orElseThrow().status());

        List<ApplicationDispatchLog> logs = service.getDispatchLogs();
        assertEquals(1, logs.size());
        assertEquals(log, logs.get(0));
    }

    public void testDomainRequiringHumanAssistanceGeneratesNeedsHumanAssistanceLog() {
        JobOpportunity job = new JobOpportunity(
                "job-2",
                "Backend Engineer",
                "Lever Tech",
                "Description",
                Set.of(),
                Set.of(),
                null,
                null,
                "Remote",
                null,
                "https://jobs.lever.co/levertech/2",
                JobStatus.DISCOVERED);
        jobRepo.save(job);

        ApplicationDispatchLog log = service.dispatchApplication("job-2", true);

        assertNotNull(log);
        assertEquals("job-2", log.jobId());
        assertEquals("NEEDS_HUMAN_ASSISTANCE", log.status());
        assertTrue(log.requiresHumanAssistance());
        assertEquals("lever.co", log.domain());
        assertNull(appRepo.loadHistory().get("job-2"));

        List<ApplicationDispatchLog> logs = service.getDispatchLogs();
        assertEquals(1, logs.size());
        assertEquals(log, logs.get(0));
    }

    public void testDomainCredentialSavingAndSubsequentDispatchSuccess() {
        JobOpportunity job = new JobOpportunity(
                "job-2",
                "Backend Engineer",
                "Lever Tech",
                "Description",
                Set.of(),
                Set.of(),
                null,
                null,
                "Remote",
                null,
                "https://jobs.lever.co/levertech/2",
                JobStatus.DISCOVERED);
        jobRepo.save(job);

        service.saveDomainCredential("lever.co", "candidate@example.com", "token_abc_123");

        Optional<CandidateCredential> cred = credentialsRepo.findByDomain("lever.co");
        assertTrue(cred.isPresent());
        assertEquals("candidate@example.com", cred.get().username());
        assertEquals("token_abc_123", cred.get().tokenOrSessionCookie());

        ApplicationDispatchLog log = service.dispatchApplication("job-2", true);
        assertEquals("DISPATCHED_SUCCESS", log.status());
        assertFalse(log.requiresHumanAssistance());
        assertEquals(JobStatus.APPLIED, appRepo.loadHistory().get("job-2"));
    }

    public void testNullChecksAndMissingJob() {
        try {
            service.dispatchApplication(null, false);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            service.dispatchApplication("non-existent-job", false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }

        try {
            service.saveDomainCredential(null, "user", "token");
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }
    }

    private static class MemoryJobRepository implements JobRepository {
        private final Map<String, JobOpportunity> jobs = new HashMap<>();

        @Override
        public void save(JobOpportunity job) {
            jobs.put(job.id(), job);
        }

        @Override
        public List<JobOpportunity> findAll() {
            return List.copyOf(jobs.values());
        }

        @Override
        public Optional<JobOpportunity> findById(String id) {
            return Optional.ofNullable(jobs.get(id));
        }
    }

    private static class MemoryJobApplicationRepository implements JobApplicationRepository {
        private final Map<String, JobStatus> history = new HashMap<>();

        @Override
        public Map<String, JobStatus> loadHistory() {
            return new HashMap<>(history);
        }

        @Override
        public void saveHistory(Map<String, JobStatus> historyMap) {
            history.clear();
            history.putAll(historyMap);
        }

        @Override
        public void updateJobStatus(String jobId, JobStatus status) {
            history.put(jobId, status);
        }
    }

    private static class MemoryCandidateCredentialsRepository implements CandidateCredentialsRepository {
        private final Map<String, CandidateCredential> credentials = new HashMap<>();

        @Override
        public Optional<CandidateCredential> findByDomain(String domain) {
            if (domain == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(credentials.get(domain));
        }

        @Override
        public void saveCredential(CandidateCredential credential) {
            credentials.put(credential.domain(), credential);
        }

        @Override
        public Map<String, CandidateCredential> findAll() {
            return Map.copyOf(credentials);
        }
    }
}
