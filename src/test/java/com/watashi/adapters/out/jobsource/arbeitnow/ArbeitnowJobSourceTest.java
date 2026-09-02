package com.watashi.adapters.out.jobsource.arbeitnow;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.infrastructure.http.HttpEngine;
import java.util.List;
import junit.framework.TestCase;

public class ArbeitnowJobSourceTest extends TestCase {

    public void testParseArbeitnowResponse() {
        String json = """
        {
          "data": [
            {
              "slug": "backend-developer-acme-123",
              "company_name": "Acme Software",
              "title": "Senior Backend Developer (Java & Spring Boot)",
              "description": "<p>We are hiring a Senior Java Developer with Spring Boot, Docker and PostgreSQL skills.</p>",
              "location": "Berlin, Germany / Remote",
              "url": "https://www.arbeitnow.com/view/backend-developer-acme-123",
              "tags": ["Java", "Spring Boot", "Docker"]
            }
          ]
        }
        """;

        List<JobOpportunity> jobs = ArbeitnowJobSource.parseJobsResponse(json);
        assertEquals(1, jobs.size());

        JobOpportunity job = jobs.get(0);
        assertEquals("arbeitnow-backend-developer-acme-123", job.id());
        assertEquals("Senior Backend Developer (Java & Spring Boot)", job.title());
        assertEquals("Acme Software", job.company());
        assertEquals(SeniorityLevel.SENIOR, job.seniorityLevel());
        assertEquals("https://www.arbeitnow.com/view/backend-developer-acme-123", job.sourceUrl());
        assertTrue(job.hasSkillNamed("Java"));
        assertTrue(job.hasSkillNamed("Spring Boot"));
        assertTrue(job.hasSkillNamed("Docker"));
    }

    public void testNullEmptyOrMalformedJson() {
        assertEquals(0, ArbeitnowJobSource.parseJobsResponse(null).size());
        assertEquals(0, ArbeitnowJobSource.parseJobsResponse("").size());
        assertEquals(0, ArbeitnowJobSource.parseJobsResponse("   ").size());
        assertEquals(0, ArbeitnowJobSource.parseJobsResponse("not json").size());
        assertEquals(
                0,
                ArbeitnowJobSource.parseJobsResponse("{\"data\": \"not array\"}")
                        .size());
    }

    public void testFetchJobsBasic() {
        ArbeitnowJobSource source = new ArbeitnowJobSource(HttpEngine.createDefault());
        assertEquals("Arbeitnow", source.getSourceName());
        List<JobOpportunity> jobs = source.fetchJobs(new JobQuery("java", "software", 5));
        assertNotNull(jobs);
    }
}
