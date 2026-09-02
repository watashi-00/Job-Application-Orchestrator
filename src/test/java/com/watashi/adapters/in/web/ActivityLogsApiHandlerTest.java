package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpServer;
import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.domain.common.SystemActivityLogEntry;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.in.GetSystemLogsUseCase;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import com.watashi.core.service.DefaultGetSystemLogsService;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import junit.framework.TestCase;

public class ActivityLogsApiHandlerTest extends TestCase {

    private HttpServer server;
    private int port;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        super.tearDown();
    }

    public void testGetUnifiedSystemLogsEndpoint() throws Exception {
        GetSystemLogsUseCase mockUseCase = new GetSystemLogsUseCase() {
            @Override
            public List<SystemActivityLogEntry> getUnifiedSystemLogs() {
                return List.of(
                        new SystemActivityLogEntry(
                                "log-1",
                                "APPLICATION_DISPATCH",
                                "Senior Developer at Acme",
                                "Submitted application",
                                "SUCCESS",
                                "2026-09-02T10:00:00Z",
                                null),
                        new SystemActivityLogEntry(
                                "log-2",
                                "RECRUITER_EMAIL",
                                "Interview Request",
                                "Would love to talk!",
                                "INTERVIEW_SCHEDULED",
                                "2026-09-02T11:00:00Z",
                                "https://mail.google.com/mail/u/0/#search/from%3Arecruiter%40acme.com"));
            }
        };

        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/logs", new ActivityLogsApiHandler(mockUseCase));
        server.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/logs"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(response.body().contains("Senior Developer at Acme"));
        assertTrue(response.body().contains("https://mail.google.com/mail/u/0/#search/from%3Arecruiter%40acme.com"));
    }

    public void testDefaultGetSystemLogsServiceMergesDispatchesAndEmails() throws Exception {
        DispatchJobApplicationUseCase mockDispatch = new DispatchJobApplicationUseCase() {
            @Override
            public ApplicationDispatchLog dispatchApplication(String jobId, boolean sendCoverLetter) {
                return null;
            }

            @Override
            public List<ApplicationDispatchLog> getDispatchLogs() {
                return List.of(new ApplicationDispatchLog(
                        "job-101",
                        "Lead Architect",
                        "TechCorp",
                        "SUCCESS",
                        "Sent email to jobs@techcorp.com",
                        "2026-09-02T08:00:00Z",
                        false,
                        "techcorp.com"));
            }

            @Override
            public void saveDomainCredential(String domain, String username, String token) {}
        };

        SyncRecruiterInboxUseCase mockInbox = new SyncRecruiterInboxUseCase() {
            @Override
            public RecruiterEmailMessage receiveIncomingEmail(
                    String sender, String recipient, String subject, String bodyText) {
                return null;
            }

            @Override
            public List<RecruiterEmailMessage> getAllReceivedEmails() {
                return List.of(new RecruiterEmailMessage(
                        "msg-1",
                        "hr@techcorp.com",
                        "me@candidate.com",
                        "Offer Letter",
                        "We are pleased to offer you...",
                        Instant.parse("2026-09-02T09:00:00Z"),
                        "job-101",
                        "TechCorp",
                        JobStatus.OFFER));
            }
        };

        GetSystemLogsUseCase service = new DefaultGetSystemLogsService(mockDispatch, mockInbox);
        List<SystemActivityLogEntry> logs = service.getUnifiedSystemLogs();

        assertEquals(2, logs.size());

        SystemActivityLogEntry emailLog = logs.stream()
                .filter(l -> "RECRUITER_EMAIL".equals(l.type()))
                .findFirst()
                .orElseThrow();
        assertEquals("Offer Letter", emailLog.title());
        assertEquals("OFFER", emailLog.status());
        assertEquals("https://mail.google.com/mail/u/0/#search/from%3Ahr%40techcorp.com", emailLog.gmailUrl());

        SystemActivityLogEntry dispatchLog = logs.stream()
                .filter(l -> "APPLICATION_DISPATCH".equals(l.type()))
                .findFirst()
                .orElseThrow();
        assertTrue(dispatchLog.title().contains("Lead Architect"));
        assertNull(dispatchLog.gmailUrl());
    }

    public void testHttpOptionsAndMethodNotAllowed() throws Exception {
        GetSystemLogsUseCase mockUseCase = () -> List.of();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/logs", new ActivityLogsApiHandler(mockUseCase));
        server.start();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest optionsReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/logs"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> optionsResp = client.send(optionsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, optionsResp.statusCode());

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/logs"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, postResp.statusCode());
    }
}
