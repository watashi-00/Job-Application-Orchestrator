package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CustomTagsApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final ManageCustomTagsUseCase tagsUseCase;

    public CustomTagsApiHandler(ManageCustomTagsUseCase tagsUseCase) {
        this.tagsUseCase = tagsUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if (tagsUseCase == null) {
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

        if ("DELETE".equalsIgnoreCase(method)) {
            handleDelete(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<CustomTag> tags = tagsUseCase.getAllTags();
        byte[] responseBytes = MAPPER.writeValueAsString(tags).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendError(exchange, 400);
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.has("name")) {
                sendError(exchange, 400);
                return;
            }

            String name = node.get("name").asText();
            String category = node.has("category") ? node.get("category").asText() : "GENERAL";
            String colorHex = node.has("colorHex") ? node.get("colorHex").asText() : "#6c757d";

            CustomTag created = tagsUseCase.addCustomTag(name, category, colorHex);

            byte[] responseBytes = MAPPER.writeValueAsString(created).getBytes(StandardCharsets.UTF_8);
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

    private void handleDelete(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String tagId = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && ("id".equalsIgnoreCase(pair[0]) || "tagId".equalsIgnoreCase(pair[0]))) {
                    tagId = pair[1];
                    break;
                }
            }
        }

        if (tagId == null) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (body.length > 0) {
                try {
                    JsonNode node = MAPPER.readTree(body);
                    if (node != null) {
                        if (node.has("id")) {
                            tagId = node.get("id").asText();
                        } else if (node.has("tagId")) {
                            tagId = node.get("tagId").asText();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (tagId == null || tagId.isBlank()) {
            sendError(exchange, 400);
            return;
        }

        tagsUseCase.removeCustomTag(tagId);

        byte[] responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

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
