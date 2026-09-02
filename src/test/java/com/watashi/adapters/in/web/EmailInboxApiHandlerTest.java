package com.watashi.adapters.in.web;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class EmailInboxApiHandlerTest extends TestCase {

    public void testGetReceivedEmails() throws Exception {
        RecruiterEmailMessage email1 =
                new RecruiterEmailMessage("sender1@test.com", "me@test.com", "Interview", "Body 1");
        List<RecruiterEmailMessage> list = List.of(email1);

        SyncRecruiterInboxUseCase mockInboxUseCase = new SyncRecruiterInboxUseCase() {
            @Override
            public RecruiterEmailMessage receiveIncomingEmail(
                    String sender, String recipient, String subject, String bodyText) {
                return null;
            }

            @Override
            public List<RecruiterEmailMessage> getAllReceivedEmails() {
                return list;
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18098, mockInboxUseCase, null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18098/api/inbox"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("sender1@test.com"));
            assertTrue(response.body().contains("Interview"));
        } finally {
            server.stop();
        }
    }

    public void testReceiveIncomingEmail() throws Exception {
        List<RecruiterEmailMessage> receivedList = new ArrayList<>();
        SyncRecruiterInboxUseCase mockInboxUseCase = new SyncRecruiterInboxUseCase() {
            @Override
            public RecruiterEmailMessage receiveIncomingEmail(
                    String sender, String recipient, String subject, String bodyText) {
                RecruiterEmailMessage msg = new RecruiterEmailMessage(sender, recipient, subject, bodyText);
                receivedList.add(msg);
                return msg;
            }

            @Override
            public List<RecruiterEmailMessage> getAllReceivedEmails() {
                return receivedList;
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18099, mockInboxUseCase, null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload =
                    "{\"sender\":\"recruiter@company.com\",\"recipient\":\"me@domain.com\",\"subject\":\"Job Offer\",\"bodyText\":\"We offer you the job!\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18099/api/inbox/parse"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(1, receivedList.size());
            assertEquals("recruiter@company.com", receivedList.get(0).sender());
            assertEquals("Job Offer", receivedList.get(0).subject());
        } finally {
            server.stop();
        }
    }

    public void testOptionsPreflight() throws Exception {
        SyncRecruiterInboxUseCase mockInboxUseCase = new SyncRecruiterInboxUseCase() {
            @Override
            public RecruiterEmailMessage receiveIncomingEmail(
                    String sender, String recipient, String subject, String bodyText) {
                return null;
            }

            @Override
            public List<RecruiterEmailMessage> getAllReceivedEmails() {
                return List.of();
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18100, mockInboxUseCase, null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18100/api/inbox"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(204, response.statusCode());
            assertEquals(
                    "*",
                    response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        } finally {
            server.stop();
        }
    }

    public void testNullInboxUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18101, (SyncRecruiterInboxUseCase) null, null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18101/api/inbox"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }
}
