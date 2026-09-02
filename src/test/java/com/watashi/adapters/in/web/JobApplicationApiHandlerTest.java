package com.watashi.adapters.in.web;

import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junit.framework.TestCase;

public class JobApplicationApiHandlerTest extends TestCase {

    public void testPostJobStatus() throws Exception {
        Map<String, JobStatus> updatedMap = new HashMap<>();
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {
                updatedMap.put(jobId, status);
            }

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return updatedMap;
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.ofNullable(updatedMap.get(jobId));
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18090, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"jobId\":\"job-123\",\"status\":\"APPLIED\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18090/api/jobs/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(JobStatus.APPLIED, updatedMap.get("job-123"));
        } finally {
            server.stop();
        }
    }

    public void testPostBatchJobStatus() throws Exception {
        Map<String, JobStatus> updatedMap = new HashMap<>();
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {
                for (String id : jobIds) {
                    updatedMap.put(id, status);
                }
            }

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return updatedMap;
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.ofNullable(updatedMap.get(jobId));
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18091, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"jobIds\":[\"job-1\",\"job-2\"],\"status\":\"IGNORED\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18091/api/jobs/batch-status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(JobStatus.IGNORED, updatedMap.get("job-1"));
            assertEquals(JobStatus.IGNORED, updatedMap.get("job-2"));
        } finally {
            server.stop();
        }
    }

    public void testGetJobHistory() throws Exception {
        Map<String, JobStatus> historyMap = Map.of("job-100", JobStatus.INTERVIEWING);
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return historyMap;
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.ofNullable(historyMap.get(jobId));
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18092, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18092/api/jobs/history"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("job-100"));
            assertTrue(response.body().contains("INTERVIEWING"));
        } finally {
            server.stop();
        }
    }

    public void testOptionsPreflightAndNonPostGetRejection() throws Exception {
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return Map.of();
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.empty();
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18093, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest optionsReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18093/api/jobs/status"))
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

            HttpRequest deleteReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18093/api/jobs/status"))
                    .DELETE()
                    .build();
            HttpResponse<String> deleteRes = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, deleteRes.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testNullTrackUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18094);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18094/api/jobs/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"jobId\":\"j1\",\"status\":\"APPLIED\"}"))
                    .build();
            HttpResponse<String> postRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(501, postRes.statusCode());

            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18094/api/jobs/history"))
                    .GET()
                    .build();
            HttpResponse<String> getRes = client.send(getReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(501, getRes.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testPostStatusMalformedJsonReturns400() throws Exception {
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return Map.of();
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.empty();
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18095, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String malformedJson = "{invalid_json}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18095/api/jobs/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(malformedJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(400, response.statusCode());
            assertEquals(
                    "*",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        } finally {
            server.stop();
        }
    }

    public void testPostStatusInvalidEnumReturns400() throws Exception {
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return Map.of();
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.empty();
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18096, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String invalidEnumJson = "{\"jobId\":\"job-123\",\"status\":\"INVALID_ENUM\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18096/api/jobs/status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(invalidEnumJson))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(400, response.statusCode());
            assertEquals(
                    "*",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        } finally {
            server.stop();
        }
    }

    public void testUnknownEndpointReturns404() throws Exception {
        TrackJobApplicationUseCase mockTrackUseCase = new TrackJobApplicationUseCase() {
            @Override
            public void updateJobStatus(String jobId, JobStatus status) {}

            @Override
            public void batchUpdateJobStatus(List<String> jobIds, JobStatus status) {}

            @Override
            public Map<String, JobStatus> getApplicationHistory() {
                return Map.of();
            }

            @Override
            public Optional<JobStatus> getJobStatus(String jobId) {
                return Optional.empty();
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18097, mockTrackUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18097/api/jobs/status/unknown"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode());
            assertEquals(
                    "*",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        } finally {
            server.stop();
        }
    }
}
