package com.watashi.core.service;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junit.framework.TestCase;

public class DefaultSyncRecruiterInboxServiceTest extends TestCase {

    private static class InMemoryRecruiterEmailRepository implements RecruiterEmailRepository {
        private final List<RecruiterEmailMessage> emails = new ArrayList<>();

        @Override
        public void save(RecruiterEmailMessage message) {
            emails.removeIf(e -> e.id().equals(message.id()));
            emails.add(message);
        }

        @Override
        public List<RecruiterEmailMessage> findAll() {
            return new ArrayList<>(emails);
        }

        @Override
        public Optional<RecruiterEmailMessage> findById(String id) {
            return emails.stream().filter(e -> e.id().equals(id)).findFirst();
        }
    }

    private static class InMemoryJobRepository implements JobRepository {
        private final List<JobOpportunity> jobs = new ArrayList<>();

        @Override
        public void save(JobOpportunity job) {
            jobs.removeIf(j -> j.id().equals(job.id()));
            jobs.add(job);
        }

        @Override
        public List<JobOpportunity> findAll() {
            return new ArrayList<>(jobs);
        }

        @Override
        public Optional<JobOpportunity> findById(String id) {
            return jobs.stream().filter(j -> j.id().equals(id)).findFirst();
        }
    }

    public void testEmailMatchingCompanyAndUpdatingStatus() {
        RecruiterEmailRepository emailRepo = new InMemoryRecruiterEmailRepository();
        JobRepository jobRepo = new InMemoryJobRepository();

        JobOpportunity job1 = new JobOpportunity(
                "job-1",
                "Backend Engineer",
                "Acme Corp",
                "Java role",
                null,
                null,
                null,
                null,
                "Remote",
                null,
                "http://acme.com/jobs/1",
                JobStatus.APPLIED);
        JobOpportunity job2 = new JobOpportunity(
                "job-2",
                "Fullstack Lead",
                "Beta Tech",
                "React role",
                null,
                null,
                null,
                null,
                "Remote",
                null,
                "http://betatech.com/jobs/2",
                JobStatus.APPLIED);
        jobRepo.save(job1);
        jobRepo.save(job2);

        SyncRecruiterInboxUseCase service = new DefaultSyncRecruiterInboxService(emailRepo, jobRepo);

        // 1. Interview email for Acme Corp
        RecruiterEmailMessage msg1 = service.receiveIncomingEmail(
                "hr@acme.com",
                "me@watashi.com",
                "Interview Request for Backend Engineer at Acme Corp",
                "We'd love to schedule a screening call.");
        assertEquals("job-1", msg1.matchedJobId());
        assertEquals("Acme Corp", msg1.matchedCompany());
        assertEquals(JobStatus.INTERVIEWING, msg1.detectedStatus());
        assertEquals(JobStatus.INTERVIEWING, jobRepo.findById("job-1").get().status());

        // 2. Offer email for Acme Corp
        RecruiterEmailMessage msg2 = service.receiveIncomingEmail(
                "hr@acme.com",
                "me@watashi.com",
                "Job Offer from Acme Corp!",
                "We are pleased to offer you the position.");
        assertEquals("job-1", msg2.matchedJobId());
        assertEquals("Acme Corp", msg2.matchedCompany());
        assertEquals(JobStatus.OFFER, msg2.detectedStatus());
        assertEquals(JobStatus.OFFER, jobRepo.findById("job-1").get().status());

        // 3. Rejection email for Beta Tech
        RecruiterEmailMessage msg3 = service.receiveIncomingEmail(
                "recruiter@betatech.com",
                "me@watashi.com",
                "Update regarding Beta Tech application",
                "Unfortunately we decided not to move forward with your application.");
        assertEquals("job-2", msg3.matchedJobId());
        assertEquals("Beta Tech", msg3.matchedCompany());
        assertEquals(JobStatus.REJECTED, msg3.detectedStatus());
        assertEquals(JobStatus.REJECTED, jobRepo.findById("job-2").get().status());

        // 4. Unknown company email
        RecruiterEmailMessage msg4 = service.receiveIncomingEmail(
                "unknown@unknown.com", "me@watashi.com", "Spam / General Email", "Hello world!");
        assertNull(msg4.matchedJobId());
        assertNull(msg4.matchedCompany());
        assertNull(msg4.detectedStatus());

        assertEquals(4, service.getAllReceivedEmails().size());
    }
}
