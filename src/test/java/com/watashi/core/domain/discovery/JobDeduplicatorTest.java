package com.watashi.core.domain.discovery;

import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

public class JobDeduplicatorTest extends TestCase {

    public void testDeduplicateIdenticalJobs() {
        JobOpportunity job1 = new JobOpportunity(
                "remotive-1",
                "Senior Java Engineer",
                "Acme Inc.",
                "Short desc",
                Set.of(),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "https://remotive.com/1",
                JobStatus.DISCOVERED);
        JobOpportunity job2 = new JobOpportunity(
                "arbeitnow-1",
                "Senior Java Engineer",
                "Acme",
                "Much longer detailed description for Senior Java Engineer position",
                Set.of(),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Worldwide",
                null,
                "https://arbeitnow.com/1",
                JobStatus.DISCOVERED);

        List<JobOpportunity> deduplicated = JobDeduplicator.deduplicate(List.of(job1, job2));

        assertEquals(1, deduplicated.size());
        assertTrue(deduplicated.get(0).description().contains("Much longer detailed"));
    }

    public void testDeduplicateNullInput() {
        List<JobOpportunity> deduplicated = JobDeduplicator.deduplicate(null);
        assertNotNull(deduplicated);
        assertTrue(deduplicated.isEmpty());
    }

    public void testDeduplicateEmptyInput() {
        List<JobOpportunity> deduplicated = JobDeduplicator.deduplicate(List.of());
        assertNotNull(deduplicated);
        assertTrue(deduplicated.isEmpty());
    }

    public void testDeduplicatePrefersRichestSalary() {
        JobOpportunity job1 = new JobOpportunity(
                "job-1",
                "Software Engineer",
                "Tech Corp",
                "Equal length desc 12345",
                Set.of(),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.HYBRID,
                "NY",
                null,
                "https://example.com/1",
                JobStatus.DISCOVERED);
        JobOpportunity job2 = new JobOpportunity(
                "job-2",
                "Software Engineer",
                "Tech",
                "Equal length desc 67890",
                Set.of(),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.HYBRID,
                "NY",
                new SalaryRange(new BigDecimal("100000"), new BigDecimal("150000"), "USD"),
                "https://example.com/2",
                JobStatus.DISCOVERED);

        List<JobOpportunity> deduplicated = JobDeduplicator.deduplicate(List.of(job1, job2));

        assertEquals(1, deduplicated.size());
        assertEquals("job-2", deduplicated.get(0).id());
        assertNotNull(deduplicated.get(0).salaryRange());
    }

    public void testCompanyAndTitleNormalization() {
        JobOpportunity job1 = new JobOpportunity(
                "1",
                "Backend Developer!",
                "Awesome Co., LLC",
                "Description 1",
                Set.of(),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url1",
                JobStatus.DISCOVERED);
        JobOpportunity job2 = new JobOpportunity(
                "2",
                "backend developer",
                "Awesome Co",
                "Description 1 plus extra details",
                Set.of(),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url2",
                JobStatus.DISCOVERED);

        List<JobOpportunity> deduplicated = JobDeduplicator.deduplicate(List.of(job1, job2));
        assertEquals(1, deduplicated.size());
        assertEquals("2", deduplicated.get(0).id());
    }
}
