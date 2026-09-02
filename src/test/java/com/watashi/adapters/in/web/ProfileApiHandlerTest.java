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

        DashboardHttpServer server = new DashboardHttpServer(18082, null, null, mockManage, null, null, mockIngest);
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
}
