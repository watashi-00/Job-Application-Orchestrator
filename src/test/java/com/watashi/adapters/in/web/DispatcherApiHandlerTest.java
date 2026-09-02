package com.watashi.adapters.in.web;

import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class DispatcherApiHandlerTest extends TestCase {

    public void testPostApplicationDispatch() throws Exception {
        List<String> dispatchedJobs = new ArrayList<>();
        DispatchJobApplicationUseCase mockUseCase = new DispatchJobApplicationUseCase() {
            @Override
            public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
                dispatchedJobs.add(jobId + ":" + sendCoverLetter);
                return new ApplicationDispatchLog(
                        jobId,
                        "Software Engineer",
                        "Acme Corp",
                        "DISPATCHED_SUCCESS",
                        "Application dispatched",
                        LocalDateTime.now().toString(),
                        false,
                        "example.com");
            }

            @Override
            public List<ApplicationDispatchLog> getDispatchLogs() {
                return List.of();
            }

            @Override
            public void saveDomainCredential(String domain, String username, String token) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18100, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "{\"jobId\":\"j-100\",\"sendCoverLetter\":true}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18100/api/applications/dispatch"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, res.statusCode());
            assertTrue(res.body().contains("\"jobId\":\"j-100\""));
            assertTrue(res.body().contains("\"status\":\"DISPATCHED_SUCCESS\""));
            assertEquals(1, dispatchedJobs.size());
            assertEquals("j-100:true", dispatchedJobs.get(0));
        } finally {
            server.stop();
        }
    }

    public void testGetApplicationLogs() throws Exception {
        ApplicationDispatchLog log1 = new ApplicationDispatchLog(
                "j-1",
                "Backend Developer",
                "Tech Inc",
                "DISPATCHED_SUCCESS",
                "Dispatched",
                "2026-09-02T10:00:00",
                false,
                "tech.com");

        DispatchJobApplicationUseCase mockUseCase = new DispatchJobApplicationUseCase() {
            @Override
            public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
                return null;
            }

            @Override
            public List<ApplicationDispatchLog> getDispatchLogs() {
                return List.of(log1);
            }

            @Override
            public void saveDomainCredential(String domain, String username, String token) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18101, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18101/api/applications/logs"))
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, res.statusCode());
            assertTrue(res.body().contains("\"jobId\":\"j-1\""));
            assertTrue(res.body().contains("\"company\":\"Tech Inc\""));
        } finally {
            server.stop();
        }
    }

    public void testPostCredentials() throws Exception {
        List<String> savedCreds = new ArrayList<>();
        DispatchJobApplicationUseCase mockUseCase = new DispatchJobApplicationUseCase() {
            @Override
            public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
                return null;
            }

            @Override
            public List<ApplicationDispatchLog> getDispatchLogs() {
                return List.of();
            }

            @Override
            public void saveDomainCredential(String domain, String username, String token) {
                savedCreds.add(domain + ":" + username + ":" + token);
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18102, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "{\"domain\":\"lever.co\",\"username\":\"dev@example.com\",\"token\":\"secret123\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18102/api/credentials"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, res.statusCode());
            assertTrue(res.body().contains("\"status\":\"ok\""));
            assertEquals(1, savedCreds.size());
            assertEquals("lever.co:dev@example.com:secret123", savedCreds.get(0));
        } finally {
            server.stop();
        }
    }

    public void testOptionsPreflightAnd501WhenUseCaseNull() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18103);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest optionsReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18103/api/applications/dispatch"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> optionsRes = client.send(optionsReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, optionsRes.statusCode());
            assertEquals(
                    "*",
                    optionsRes
                            .headers()
                            .firstValue("Access-Control-Allow-Origin")
                            .orElse(null));

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18103/api/applications/dispatch"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"jobId\":\"j-1\"}"))
                    .build();
            HttpResponse<String> postRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(501, postRes.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testMalformedJsonReturns400() throws Exception {
        DispatchJobApplicationUseCase mockUseCase = new DispatchJobApplicationUseCase() {
            @Override
            public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
                return null;
            }

            @Override
            public List<ApplicationDispatchLog> getDispatchLogs() {
                return List.of();
            }

            @Override
            public void saveDomainCredential(String domain, String username, String token) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18104, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18104/api/applications/dispatch"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{bad_json}"))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(400, res.statusCode());
        } finally {
            server.stop();
        }
    }
}
