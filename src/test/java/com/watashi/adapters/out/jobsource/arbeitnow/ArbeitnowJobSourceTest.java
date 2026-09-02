package com.watashi.adapters.out.jobsource.arbeitnow;

import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.infrastructure.http.HttpEngine;
import com.watashi.infrastructure.http.RequestSpec;
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
        HttpEngine stubEngine = new HttpEngine() {
            @Override
            public CompletableFuture<HttpResponse<String>> fetch(RequestSpec requestSpec) {
                return CompletableFuture.completedFuture(createMockResponse(200, json));
            }
        };

        ArbeitnowJobSource source = new ArbeitnowJobSource(stubEngine);
        assertEquals("Arbeitnow", source.getSourceName());
        List<JobOpportunity> jobs = source.fetchJobs(new JobQuery("java", "software", 5));
        assertNotNull(jobs);
        assertEquals(1, jobs.size());
        assertEquals("arbeitnow-backend-developer-acme-123", jobs.get(0).id());
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
