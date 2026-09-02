package com.watashi.adapters.in.web;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.ports.in.ManageAgentConfigUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

public class AgentConfigApiHandlerTest extends TestCase {

    public void testGetAgentConfig() throws Exception {
        AgentConfig config =
                new AgentConfig("ollama", "http://localhost:11434", "llama3", AgentConfig.ModelTier.SMALL, "", 0);
        ManageAgentConfigUseCase mockUseCase = new ManageAgentConfigUseCase() {
            @Override
            public AgentConfig getAgentConfig() {
                return config;
            }

            @Override
            public void updateAgentConfig(AgentConfig config) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18120, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18120/api/agent/config"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("ollama"));
            assertTrue(response.body().contains("llama3"));
            assertTrue(response.body().contains("SMALL"));
        } finally {
            server.stop();
        }
    }

    public void testUpdateAgentConfig() throws Exception {
        AtomicReference<AgentConfig> savedConfig = new AtomicReference<>();
        ManageAgentConfigUseCase mockUseCase = new ManageAgentConfigUseCase() {
            @Override
            public AgentConfig getAgentConfig() {
                return savedConfig.get();
            }

            @Override
            public void updateAgentConfig(AgentConfig config) {
                savedConfig.set(config);
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18121, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload =
                    "{\"provider\":\"ollama\",\"ollamaUrl\":\"http://localhost:11434\",\"modelName\":\"mistral\",\"modelTier\":\"MEDIUM\",\"apiKey\":\"secret-key\",\"customChunkSize\":500}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18121/api/agent/config"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertNotNull(savedConfig.get());
            assertEquals("mistral", savedConfig.get().modelName());
            assertEquals(AgentConfig.ModelTier.MEDIUM, savedConfig.get().modelTier());
            assertEquals("secret-key", savedConfig.get().apiKey());
        } finally {
            server.stop();
        }
    }

    public void testDeleteMethodReturns405() throws Exception {
        ManageAgentConfigUseCase mockUseCase = new ManageAgentConfigUseCase() {
            @Override
            public AgentConfig getAgentConfig() {
                return AgentConfig.defaultConfig();
            }

            @Override
            public void updateAgentConfig(AgentConfig config) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18122, mockUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18122/api/agent/config"))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
        } finally {
            server.stop();
        }
    }

    public void testNullUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18123, (ManageAgentConfigUseCase) null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18123/api/agent/config"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }
}
