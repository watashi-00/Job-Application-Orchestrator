package com.watashi.adapters.in.web;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class ProfileApiHandlerTest extends TestCase {

    public void testPostProfilePdfUpload() throws Exception {
        CandidateProfile mockProfile =
                new CandidateProfile("c1", "Senior Java Dev", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
        IngestCandidateProfileUseCase mockIngest = (bytes, prefs) -> mockProfile;
        ManageCandidateProfileUseCase mockManage = new ManageCandidateProfileUseCase() {
            private CandidateProfile profile = mockProfile;

            public Optional<CandidateProfile> getProfile() {
                return Optional.ofNullable(profile);
            }

            public void updateProfile(CandidateProfile p) {
                this.profile = p;
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18082, null, null, mockManage, mockIngest);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18082/api/profile"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray("dummy pdf bytes".getBytes()))
                    .header("Content-Type", "application/pdf")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Senior Java Dev"));
        } finally {
            server.stop();
        }
    }

    public void testPostProfilePayloadExceedsLimit() throws Exception {
        CandidateProfile mockProfile =
                new CandidateProfile("c1", "Senior Java Dev", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
        IngestCandidateProfileUseCase mockIngest = (bytes, prefs) -> mockProfile;
        ManageCandidateProfileUseCase mockManage = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.ofNullable(mockProfile);
            }

            public void updateProfile(CandidateProfile p) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18083, null, null, mockManage, mockIngest);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            byte[] oversizedBytes = new byte[ProfileApiHandler.MAX_PDF_SIZE_BYTES + 1];
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18083/api/profile"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(oversizedBytes))
                    .header("Content-Type", "application/pdf")
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

    public void testPostProfileCorruptPdfIngestionError() throws Exception {
        IngestCandidateProfileUseCase mockIngest = (bytes, prefs) -> {
            throw new IllegalArgumentException("Corrupt PDF file header");
        };
        ManageCandidateProfileUseCase mockManage = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.empty();
            }

            public void updateProfile(CandidateProfile p) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18084, null, null, mockManage, mockIngest);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18084/api/profile"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray("invalid pdf content".getBytes()))
                    .header("Content-Type", "application/pdf")
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

    public void testPostProfileMissingIngestUseCaseReturns501() throws Exception {
        ManageCandidateProfileUseCase mockManage = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.empty();
            }

            public void updateProfile(CandidateProfile p) {}
        };

        DashboardHttpServer server =
                new DashboardHttpServer(18085, null, null, mockManage, (IngestCandidateProfileUseCase) null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18085/api/profile"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray("dummy pdf bytes".getBytes()))
                    .header("Content-Type", "application/pdf")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
            assertEquals(
                    "*",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        } finally {
            server.stop();
        }
    }

    public void testPostProfileUpdateEndpoint() throws Exception {
        CandidateProfile mockProfile = new CandidateProfile(
                "c1", "Initial Title", "Initial Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
        ManageCandidateProfileUseCase mockManage = new ManageCandidateProfileUseCase() {
            private CandidateProfile profile = mockProfile;

            public Optional<CandidateProfile> getProfile() {
                return Optional.ofNullable(profile);
            }

            public void updateProfile(CandidateProfile p) {
                this.profile = p;
            }
        };

        DashboardHttpServer server =
                new DashboardHttpServer(18086, null, null, mockManage, (IngestCandidateProfileUseCase) null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload =
                    "{\"title\":\"Staff Software Engineer\",\"summary\":\"Updated Summary\",\"skills\":[\"Java\",\"Spring\"],\"targetSeniorities\":[\"SENIOR\",\"LEAD\"],\"preferredWorkModes\":[\"REMOTE\"]}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18086/api/profile/update"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Staff Software Engineer"));
            assertTrue(response.body().contains("Updated Summary"));
            assertTrue(response.body().contains("Java"));
            assertTrue(response.body().contains("SENIOR"));
            assertTrue(response.body().contains("REMOTE"));

            CandidateProfile updated = mockManage.getProfile().orElseThrow();
            assertEquals("Staff Software Engineer", updated.title());
            assertEquals("Updated Summary", updated.summary());
            assertEquals(2, updated.skills().size());
            assertEquals(2, updated.targetSeniorities().size());
            assertEquals(1, updated.preferredWorkModes().size());
        } finally {
            server.stop();
        }
    }
}
