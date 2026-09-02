package com.watashi.adapters.out.jobsource.jobicy;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.infrastructure.http.HttpEngine;
import java.math.BigDecimal;
import java.util.List;
import junit.framework.TestCase;

public class JobicyJobSourceTest extends TestCase {

    public void testParseJobicyResponse() {
        String json = """
        {
          "jobs": [
            {
              "id": 98765,
              "jobTitle": "Lead Java Engineer",
              "companyName": "TechCorp",
              "jobDescription": "Looking for Lead Java Engineer proficient in Java, AWS, Microservices, and PostgreSQL.",
              "jobGeo": "Worldwide",
              "annualSalaryMin": "140000",
              "annualSalaryMax": "180000",
              "salaryCurrency": "USD",
              "url": "https://jobicy.com/jobs/98765"
            }
          ]
        }
        """;

        List<JobOpportunity> jobs = JobicyJobSource.parseJobsResponse(json);
        assertEquals(1, jobs.size());

        JobOpportunity job = jobs.get(0);
        assertEquals("jobicy-98765", job.id());
        assertEquals("Lead Java Engineer", job.title());
        assertEquals("TechCorp", job.company());
        assertEquals(SeniorityLevel.LEAD, job.seniorityLevel());
        assertTrue(job.hasSkillNamed("Java"));
        assertTrue(job.hasSkillNamed("AWS"));
        assertNotNull(job.salaryRange());
        assertEquals(new BigDecimal("140000"), job.salaryRange().min());
        assertEquals(new BigDecimal("180000"), job.salaryRange().max());
        assertEquals("USD", job.salaryRange().currency());
    }

    public void testNullEmptyOrMalformedJson() {
        assertEquals(0, JobicyJobSource.parseJobsResponse(null).size());
        assertEquals(0, JobicyJobSource.parseJobsResponse("").size());
        assertEquals(0, JobicyJobSource.parseJobsResponse("   ").size());
        assertEquals(0, JobicyJobSource.parseJobsResponse("not json").size());
        assertEquals(
                0,
                JobicyJobSource.parseJobsResponse("{\"jobs\": \"not array\"}").size());
    }

    public void testFetchJobsBasic() {
        JobicyJobSource source = new JobicyJobSource(HttpEngine.createDefault());
        assertEquals("Jobicy", source.getSourceName());
        List<JobOpportunity> jobs = source.fetchJobs(new JobQuery("java", "software", 5));
        assertNotNull(jobs);
    }
}
