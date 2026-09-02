package com.watashi.adapters.in.web;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

public class DashboardHttpServerTest extends TestCase {

    private DashboardHttpServer server;
    private int port = 18080;

    private AtomicReference<CandidateProfile> testProfile;
    private List<JobOpportunity> testJobs;
    private AtomicBoolean discoverCalled;

    private ManageCandidateProfileUseCase profileUseCase;
    private DiscoverJobsUseCase discoverUseCase;
    private GetJobsUseCase getJobsUseCase;

    protected void setUp() throws Exception {
        testProfile = new AtomicReference<>(new CandidateProfile(
                "cand-1", "Senior Java Engineer", "Backend Specialist", Set.of(), Set.of(), Set.of(), null, Set.of()));

        testJobs = new ArrayList<>();
        testJobs.add(new JobOpportunity(
                "job-1",
                "Software Engineer",
                "TechCorp",
                "Description",
                Set.of(),
                Set.of(),
                null,
                null,
                "Remote",
                null,
                "https://example.com/job1",
                JobStatus.DISCOVERED));

        discoverCalled = new AtomicBoolean(false);

        profileUseCase = new ManageCandidateProfileUseCase() {
            @Override
            public Optional<CandidateProfile> getProfile() {
                return Optional.ofNullable(testProfile.get());
            }

            @Override
            public void updateProfile(CandidateProfile profile) {
                testProfile.set(profile);
            }
        };

        discoverUseCase = (profile, config) -> {
            discoverCalled.set(true);
            return List.of();
        };

        getJobsUseCase = () -> testJobs;

        server = new DashboardHttpServer(port, discoverUseCase, null, profileUseCase, getJobsUseCase);
        server.start();
    }

    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    public void testGetJobs() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/jobs"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(response.body().contains("Software Engineer"));
    }

    public void testPostJobs() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/jobs"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(discoverCalled.get());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    public void testOptionsJobs() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/jobs"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals(
                "GET, POST, OPTIONS",
                response.headers().firstValue("Access-Control-Allow-Methods").orElse(null));
        assertEquals(
                "Content-Type",
                response.headers().firstValue("Access-Control-Allow-Headers").orElse(null));
    }

    public void testGetProfileSuccess() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/profile"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(response.body().contains("Senior Java Engineer"));
    }

    public void testGetProfileNotFound() throws Exception {
        testProfile.set(null);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/profile"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    public void testMethodNotAllowed() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // PUT /api/jobs -> 405
        HttpRequest putJobsReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/jobs"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> putJobsResp = client.send(putJobsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, putJobsResp.statusCode());

        // POST /api/profile -> 405
        HttpRequest postProfileReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/profile"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> postProfileResp = client.send(postProfileReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, postProfileResp.statusCode());
    }

    public void testServerLifecycleShutdown() throws Exception {
        int testPort = 18081;
        DashboardHttpServer testServer =
                new DashboardHttpServer(testPort, discoverUseCase, null, profileUseCase, getJobsUseCase);
        testServer.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + testPort + "/api/jobs"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        testServer.stop();
    }
}
