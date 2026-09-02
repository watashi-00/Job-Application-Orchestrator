package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobApplicationApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final TrackJobApplicationUseCase trackUseCase;

    public JobApplicationApiHandler(TrackJobApplicationUseCase trackUseCase) {
        this.trackUseCase = trackUseCase;
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

        if (trackUseCase == null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && path.endsWith("/history")) {
            handleGetHistory(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/batch-status")) {
            handlePostBatchStatus(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/status")) {
            handlePostStatus(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private void handleGetHistory(HttpExchange exchange) throws IOException {
        Map<String, JobStatus> history = trackUseCase.getApplicationHistory();
        byte[] responseBytes = MAPPER.writeValueAsString(history).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void handlePostStatus(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.has("jobId") || !node.has("status")) {
                sendError(exchange, 400);
                return;
            }

            String jobId = node.get("jobId").asText();
            String statusStr = node.get("status").asText();
            JobStatus status = JobStatus.valueOf(statusStr);

            trackUseCase.updateJobStatus(jobId, status);

            byte[] responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400);
        }
    }

    private void handlePostBatchStatus(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null
                    || !node.has("jobIds")
                    || !node.has("status")
                    || !node.get("jobIds").isArray()) {
                sendError(exchange, 400);
                return;
            }

            List<String> jobIds = new ArrayList<>();
            for (JsonNode idNode : node.get("jobIds")) {
                jobIds.add(idNode.asText());
            }

            String statusStr = node.get("status").asText();
            JobStatus status = JobStatus.valueOf(statusStr);

            trackUseCase.batchUpdateJobStatus(jobIds, status);

            byte[] responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
