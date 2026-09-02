package com.watashi.adapters.in.web;

import com.watashi.core.ports.in.AgentChatUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import junit.framework.TestCase;

public class AgentChatApiHandlerTest extends TestCase {

    public void testChatSuccess() throws Exception {
        AgentChatUseCase mockUseCase = new AgentChatUseCase() {
            @Override
            public String chat(String message) {
                return "Agent response to: " + message;
            }

            @Override
            public AgentChatResponse chat(AgentChatRequest request) {
                return new AgentChatResponse("Agent response to: " + request.message(), List.of("searchJobs"));
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18110, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"message\":\"Search for java jobs\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18110/api/agent/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Agent response to: Search for java jobs"));
            assertTrue(response.body().contains("searchJobs"));
        } finally {
            server.stop();
        }
    }

    public void testChatEmptyMessageReturns400() throws Exception {
        AgentChatUseCase mockUseCase = new AgentChatUseCase() {
            @Override
            public String chat(String message) {
                return "";
            }

            @Override
            public AgentChatResponse chat(AgentChatRequest request) {
                return new AgentChatResponse("");
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18111, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"message\":\"\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18111/api/agent/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(400, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testChatGetMethodReturns405() throws Exception {
        AgentChatUseCase mockUseCase = new AgentChatUseCase() {
            @Override
            public String chat(String message) {
                return "ok";
            }

            @Override
            public AgentChatResponse chat(AgentChatRequest request) {
                return new AgentChatResponse("ok");
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18112, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18112/api/agent/chat"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testNullUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18113, (AgentChatUseCase) null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18113/api/agent/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hi\"}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }
}
