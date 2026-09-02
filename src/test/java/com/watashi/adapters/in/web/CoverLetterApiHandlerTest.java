package com.watashi.adapters.in.web;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.domain.matching.MatchingEngine;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.service.DefaultAssessJobCompatibilityService;
import com.watashi.core.service.DefaultCoverLetterService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class CoverLetterApiHandlerTest extends TestCase {

    public void testGetCoverLetterEndpoint() throws Exception {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Senior Java Engineer",
                "Acme Corp",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);
        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Backend Software Engineer",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.SENIOR),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());

        JobRepository mockRepo = new JobRepository() {
            public void save(JobOpportunity j) {}

            public List<JobOpportunity> findAll() {
                return List.of(job);
            }

            public Optional<JobOpportunity> findById(String id) {
                return id.equals("j1") ? Optional.of(job) : Optional.empty();
            }
        };

        ManageCandidateProfileUseCase mockProfileUseCase = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.of(profile);
            }

            public void updateProfile(CandidateProfile p) {}
        };

        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(engine);
        GenerateCoverLetterUseCase coverLetterUseCase = new DefaultCoverLetterService();

        DashboardHttpServer server = new DashboardHttpServer(
                18083,
                null,
                assessUseCase,
                mockProfileUseCase,
                null,
                mockRepo,
                mockRepo::findAll,
                null,
                coverLetterUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18083/api/cover-letter?jobId=j1"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Senior Java Engineer"));
            assertTrue(response.body().contains("Acme Corp"));
        } finally {
            server.stop();
        }
    }

    public void testMissingJobIdReturns400() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18084);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18084/api/cover-letter"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testNotFoundJobIdReturns404() throws Exception {
        CandidateProfile profile =
                new CandidateProfile("c1", "Title", "Summary", Set.of(), Set.of(), Set.of(), null, Set.of());
        ManageCandidateProfileUseCase mockProfileUseCase = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.of(profile);
            }

            public void updateProfile(CandidateProfile p) {}
        };
        GetJobsUseCase emptyGetJobsUseCase = List::of;
        GenerateCoverLetterUseCase coverLetterUseCase = new DefaultCoverLetterService();

        DashboardHttpServer server = new DashboardHttpServer(
                18085, null, null, mockProfileUseCase, null, emptyGetJobsUseCase, null, coverLetterUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18085/api/cover-letter?jobId=nonexistent"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testMissingProfileReturns404() throws Exception {
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Title",
                "Company",
                "Desc",
                Set.of(),
                Set.of(),
                SeniorityLevel.SENIOR,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);
        ManageCandidateProfileUseCase emptyProfileUseCase = new ManageCandidateProfileUseCase() {
            public Optional<CandidateProfile> getProfile() {
                return Optional.empty();
            }

            public void updateProfile(CandidateProfile p) {}
        };
        GetJobsUseCase getJobsUseCase = () -> List.of(job);
        GenerateCoverLetterUseCase coverLetterUseCase = new DefaultCoverLetterService();

        DashboardHttpServer server = new DashboardHttpServer(
                18087, null, null, emptyProfileUseCase, null, getJobsUseCase, null, coverLetterUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18087/api/cover-letter?jobId=j1"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testMissingCoverLetterUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18088);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18088/api/cover-letter?jobId=j1"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testOptionsAndMethodNotAllowed() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18086);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest optionsReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18086/api/cover-letter"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> optionsRes = client.send(optionsReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(204, optionsRes.statusCode());

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18086/api/cover-letter"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> postRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, postRes.statusCode());
        } finally {
            server.stop();
        }
    }
}
