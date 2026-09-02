package com.watashi.adapters.out.jobsource.jobicy;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.infrastructure.http.HttpEngine;
import com.watashi.infrastructure.http.RequestSpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLSession;
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
        HttpEngine stubEngine = new HttpEngine() {
            @Override
            public CompletableFuture<HttpResponse<String>> fetch(RequestSpec requestSpec) {
                return CompletableFuture.completedFuture(createMockResponse(200, json));
            }
        };

        JobicyJobSource source = new JobicyJobSource(stubEngine);
        assertEquals("Jobicy", source.getSourceName());
        List<JobOpportunity> jobs = source.fetchJobs(new JobQuery("java", "software", 5));
        assertNotNull(jobs);
        assertEquals(1, jobs.size());
        assertEquals("jobicy-98765", jobs.get(0).id());
    }

    private static HttpResponse<String> createMockResponse(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (k, v) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
