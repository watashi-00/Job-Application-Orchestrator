package com.watashi.adapters.out.jobsource.remotive;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.ports.out.JobSource;
import com.watashi.infrastructure.http.HttpEngine;
import java.math.BigDecimal;
import java.util.List;
import junit.framework.TestCase;

public class RemotiveJobSourceTest extends TestCase {

    public void testRemotiveJobSourceParsing() {
        HttpEngine httpEngine = HttpEngine.createDefault();
        JobSource source = new RemotiveJobSource(httpEngine);

        assertEquals("Remotive", source.getSourceName());

        String sampleJson = """
        {
          "jobs": [
            {
              "id": 1823901,
              "url": "https://remotive.com/remote-jobs/software-dev/senior-java-developer-1823901",
              "title": "Senior Java Developer",
              "company_name": "Acme Corp",
              "category": "Software Development",
              "tags": ["java", "spring boot", "postgres"],
              "job_type": "full_time",
              "publication_date": "2026-09-01T12:00:00",
              "candidate_required_location": "Worldwide",
              "salary": "$120,000 - $150,000",
              "description": "<p>We are looking for a Senior Java Developer with Spring Boot experience.</p>"
            }
          ]
        }
        """;

        List<JobOpportunity> jobs = RemotiveJobSource.parseJobsResponse(sampleJson);
        assertEquals(1, jobs.size());

        JobOpportunity job = jobs.get(0);
        assertEquals("remotive-1823901", job.id());
        assertEquals("Senior Java Developer", job.title());
        assertEquals("Acme Corp", job.company());
        assertNotNull(job.salaryRange());
        assertEquals(new BigDecimal("120000"), job.salaryRange().min());
        assertEquals(new BigDecimal("150000"), job.salaryRange().max());
        assertEquals("USD", job.salaryRange().currency());
        assertEquals(SeniorityLevel.SENIOR, job.seniorityLevel());
        assertEquals("Worldwide", job.location());
        assertEquals("https://remotive.com/remote-jobs/software-dev/senior-java-developer-1823901", job.sourceUrl());
        assertTrue(job.requiredSkills().stream().anyMatch(s -> s.matchesName("Java")));
        assertTrue(job.requiredSkills().stream().anyMatch(s -> s.matchesName("Spring Boot")));
    }

    public void testNullEmptyOrWhitespaceJson() {
        assertEquals(0, RemotiveJobSource.parseJobsResponse(null).size());
        assertEquals(0, RemotiveJobSource.parseJobsResponse("").size());
        assertEquals(0, RemotiveJobSource.parseJobsResponse("   \n\t  ").size());
    }

    public void testMalformedJson() {
        assertEquals(0, RemotiveJobSource.parseJobsResponse("not json at all").size());
        assertEquals(
                0,
                RemotiveJobSource.parseJobsResponse("{ \"jobs\": \"invalid\" }").size());
        assertEquals(
                0,
                RemotiveJobSource.parseJobsResponse("{ \"jobs\": [{ \"id\": }").size());
    }

    public void testMissingOptionalFields() {
        String jsonWithoutOptionalFields = """
        {
          "jobs": [
            {
              "id": 999111,
              "url": "https://remotive.com/remote-jobs/backend-dev-999111",
              "title": "Backend Engineer",
              "company_name": "Tech Corp",
              "description": "<p>We are hiring a Backend Engineer.</p>"
            }
          ]
        }
        """;

        List<JobOpportunity> jobs = RemotiveJobSource.parseJobsResponse(jsonWithoutOptionalFields);
        assertEquals(1, jobs.size());

        JobOpportunity job = jobs.get(0);
        assertEquals("remotive-999111", job.id());
        assertEquals("Backend Engineer", job.title());
        assertEquals("Tech Corp", job.company());
        assertNull(job.salaryRange());
        assertEquals("", job.location());
        assertEquals("https://remotive.com/remote-jobs/backend-dev-999111", job.sourceUrl());
    }

    public void testCategoryUrlEncodingInFetchJobs() {
        JobSource source = new RemotiveJobSource(HttpEngine.createDefault());
        JobQuery queryWithSpaces = new JobQuery("java", "software dev & qa", 10);
        List<JobOpportunity> jobs = source.fetchJobs(queryWithSpaces);
        assertNotNull(jobs);
    }
}
