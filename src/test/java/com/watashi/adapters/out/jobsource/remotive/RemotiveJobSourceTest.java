package com.watashi.adapters.out.jobsource.remotive;

import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.ports.out.JobSource;
import com.watashi.infrastructure.http.HttpEngine;
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
        assertTrue(job.requiredSkills().stream().anyMatch(s -> s.matchesName("Java")));
        assertTrue(job.requiredSkills().stream().anyMatch(s -> s.matchesName("Spring Boot")));
    }
}
