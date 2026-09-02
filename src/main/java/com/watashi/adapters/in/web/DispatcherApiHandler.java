package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.core.domain.application.ApplicationDispatchLog;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DispatcherApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DispatchJobApplicationUseCase dispatchUseCase;

    public DispatcherApiHandler(DispatchJobApplicationUseCase dispatchUseCase) {
        this.dispatchUseCase = dispatchUseCase;
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
            sendError(exchange, 405);
            return;
        }

        if (dispatchUseCase == null) {
            sendError(exchange, 501);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/applications/dispatch")) {
            handlePostDispatch(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(method) && path.endsWith("/applications/logs")) {
            handleGetLogs(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/credentials")) {
            handlePostCredentials(exchange);
            return;
        }

        sendError(exchange, 404);
    }

    private void handlePostDispatch(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.has("jobId") || node.get("jobId").asText().isBlank()) {
                sendError(exchange, 400);
                return;
            }

            String jobId = node.get("jobId").asText();
            boolean sendCoverLetter =
                    node.has("sendCoverLetter") && node.get("sendCoverLetter").asBoolean(false);

            ApplicationDispatchLog log = dispatchUseCase.dispatchApplication(jobId, sendCoverLetter);
            byte[] responseBytes = MAPPER.writeValueAsString(log).getBytes(StandardCharsets.UTF_8);

            sendJsonResponse(exchange, 200, responseBytes);
        } catch (Exception e) {
            sendError(exchange, 400);
        }
    }

    private void handleGetLogs(HttpExchange exchange) throws IOException {
        try {
            List<ApplicationDispatchLog> logs = dispatchUseCase.getDispatchLogs();
            byte[] responseBytes = MAPPER.writeValueAsString(logs).getBytes(StandardCharsets.UTF_8);
            sendJsonResponse(exchange, 200, responseBytes);
        } catch (Exception e) {
            sendError(exchange, 500);
        }
    }

    private void handlePostCredentials(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null
                    || !node.has("domain")
                    || !node.has("username")
                    || !node.has("token")
                    || node.get("domain").asText().isBlank()
                    || node.get("username").asText().isBlank()
                    || node.get("token").asText().isBlank()) {
                sendError(exchange, 400);
                return;
            }

            String domain = node.get("domain").asText();
            String username = node.get("username").asText();
            String token = node.get("token").asText();

            dispatchUseCase.saveDomainCredential(domain, username, token);

            byte[] responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            sendJsonResponse(exchange, 200, responseBytes);
        } catch (Exception e) {
            sendError(exchange, 400);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, byte[] responseBytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
