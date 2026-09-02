package com.watashi.core.service;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
import com.watashi.core.ports.out.JobApplicationRepository;
import com.watashi.core.ports.out.JobRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultTrackJobApplicationServiceTest extends TestCase {

    private MemoryJobApplicationRepository appRepo;
    private MemoryJobRepository jobRepo;
    private TrackJobApplicationUseCase service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        appRepo = new MemoryJobApplicationRepository();
        jobRepo = new MemoryJobRepository();
        service = new DefaultTrackJobApplicationService(appRepo, jobRepo);
    }

    public void testUpdateJobStatusWithJobRepository() {
        JobOpportunity job = new JobOpportunity(
                "job-1",
                "Software Engineer",
                "Tech Corp",
                "Description",
                Set.of(),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "https://example.com/job/1",
                JobStatus.DISCOVERED);
        jobRepo.save(job);

        service.updateJobStatus("job-1", JobStatus.APPLIED);

        assertEquals(JobStatus.APPLIED, appRepo.loadHistory().get("job-1"));
        assertEquals(Optional.of(JobStatus.APPLIED), service.getJobStatus("job-1"));
        assertEquals(JobStatus.APPLIED, jobRepo.findById("job-1").orElseThrow().status());
    }

    public void testUpdateJobStatusWithoutJobRepository() {
        TrackJobApplicationUseCase serviceWithoutJobRepo = new DefaultTrackJobApplicationService(appRepo);
        serviceWithoutJobRepo.updateJobStatus("job-1", JobStatus.INTERVIEWING);

        assertEquals(JobStatus.INTERVIEWING, appRepo.loadHistory().get("job-1"));
        assertEquals(Optional.of(JobStatus.INTERVIEWING), serviceWithoutJobRepo.getJobStatus("job-1"));
    }

    public void testBatchUpdateJobStatus() {
        JobOpportunity job1 = new JobOpportunity(
                "job-1",
                "Dev 1",
                "Co 1",
                "Desc",
                Set.of(),
                Set.of(),
                null,
                null,
                null,
                null,
                null,
                JobStatus.DISCOVERED);
        JobOpportunity job2 = new JobOpportunity(
                "job-2",
                "Dev 2",
                "Co 2",
                "Desc",
                Set.of(),
                Set.of(),
                null,
                null,
                null,
                null,
                null,
                JobStatus.DISCOVERED);
        jobRepo.save(job1);
        jobRepo.save(job2);

        service.batchUpdateJobStatus(List.of("job-1", "job-2"), JobStatus.IGNORED);

        assertEquals(JobStatus.IGNORED, appRepo.loadHistory().get("job-1"));
        assertEquals(JobStatus.IGNORED, appRepo.loadHistory().get("job-2"));
        assertEquals(JobStatus.IGNORED, jobRepo.findById("job-1").orElseThrow().status());
        assertEquals(JobStatus.IGNORED, jobRepo.findById("job-2").orElseThrow().status());
    }

    public void testGetApplicationHistoryAndGetJobStatus() {
        service.updateJobStatus("job-1", JobStatus.OFFER);
        service.updateJobStatus("job-2", JobStatus.DELETED);

        Map<String, JobStatus> history = service.getApplicationHistory();
        assertEquals(2, history.size());
        assertEquals(JobStatus.OFFER, history.get("job-1"));
        assertEquals(JobStatus.DELETED, history.get("job-2"));

        assertEquals(Optional.of(JobStatus.OFFER), service.getJobStatus("job-1"));
        assertEquals(Optional.empty(), service.getJobStatus("job-non-existent"));
    }

    public void testNullChecks() {
        try {
            new DefaultTrackJobApplicationService(null);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            service.updateJobStatus(null, JobStatus.APPLIED);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            service.updateJobStatus("job-1", null);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            service.batchUpdateJobStatus(null, JobStatus.APPLIED);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
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
            if (historyMap == null) {
                throw new NullPointerException("historyMap cannot be null");
            }
            history.clear();
            history.putAll(historyMap);
        }

        @Override
        public void updateJobStatus(String jobId, JobStatus status) {
            if (jobId == null || status == null) {
                throw new NullPointerException("jobId and status cannot be null");
            }
            history.put(jobId, status);
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
}
