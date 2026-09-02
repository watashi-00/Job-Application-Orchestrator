package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.agent.AgentConfig;
import com.watashi.core.ports.in.ManageAgentConfigUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AgentConfigApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final ManageAgentConfigUseCase manageAgentConfigUseCase;

    public AgentConfigApiHandler(ManageAgentConfigUseCase manageAgentConfigUseCase) {
        this.manageAgentConfigUseCase = manageAgentConfigUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if (manageAgentConfigUseCase == null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
            return;
        }

        sendError(exchange, 404);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        AgentConfig config = manageAgentConfigUseCase.getAgentConfig();
        if (config == null) {
            config = AgentConfig.defaultConfig();
        }
        byte[] responseBytes = MAPPER.writeValueAsString(config).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        if (bodyBytes.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            AgentConfig newConfig = MAPPER.readValue(bodyBytes, AgentConfig.class);
            manageAgentConfigUseCase.updateAgentConfig(newConfig);

            AgentConfig updated = manageAgentConfigUseCase.getAgentConfig();
            if (updated == null) {
                updated = newConfig;
            }

            byte[] responseBytes = MAPPER.writeValueAsString(updated).getBytes(StandardCharsets.UTF_8);

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
