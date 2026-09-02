package com.watashi.adapters.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.ports.out.LlmProviderPort;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;

public class OllamaLlmProviderTest extends TestCase {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/generate", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    byte[] requestBytes = exchange.getRequestBody().readAllBytes();
                    JsonNode jsonNode = objectMapper.readTree(requestBytes);
                    String model = jsonNode.get("model").asText();
                    String prompt = jsonNode.get("prompt").asText();
                    boolean stream = jsonNode.get("stream").asBoolean();

                    if ("llama3".equals(model) && "Test prompt".equals(prompt) && !stream) {
                        String jsonResponse =
                                "{\"model\":\"llama3\",\"response\":\"Generated LLM output\",\"done\":true}";
                        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, responseBytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(responseBytes);
                        }
                        return;
                    }
                }
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
            }
        });
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        super.tearDown();
    }

    public void testGenerateSuccess() {
        LlmProviderPort provider = new OllamaLlmProvider();
        AgentConfig config = new AgentConfig("ollama", "http://127.0.0.1:" + port, "llama3", ModelTier.SMALL, "", 0);

        String result = provider.generate("Test prompt", config);
        assertEquals("Generated LLM output", result);
    }
}
