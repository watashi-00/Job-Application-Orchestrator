package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.agent.AgentConfig;
import com.watashi.core.ports.in.AgentChatUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AgentChatApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final AgentChatUseCase agentChatUseCase;

    public AgentChatApiHandler(AgentChatUseCase agentChatUseCase) {
        this.agentChatUseCase = agentChatUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"POST".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if (agentChatUseCase == null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
            return;
        }

        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        if (bodyBytes.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(bodyBytes);
            if (node == null || !node.has("message")) {
                sendError(exchange, 400);
                return;
            }

            String message = node.get("message").asText();
            if (message == null || message.isBlank()) {
                sendError(exchange, 400);
                return;
            }

            String context = node.has("context") && !node.get("context").isNull()
                    ? node.get("context").asText()
                    : null;
            AgentConfig configOverride =
                    node.has("configOverride") && !node.get("configOverride").isNull()
                            ? MAPPER.treeToValue(node.get("configOverride"), AgentConfig.class)
                            : null;

            AgentChatUseCase.AgentChatRequest request =
                    new AgentChatUseCase.AgentChatRequest(message, context, configOverride);
            AgentChatUseCase.AgentChatResponse response = agentChatUseCase.chat(request);

            byte[] responseBytes = MAPPER.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            sendError(exchange, 400);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
