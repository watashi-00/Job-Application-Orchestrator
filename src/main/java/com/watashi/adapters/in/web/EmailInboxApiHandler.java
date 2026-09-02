package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EmailInboxApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final SyncRecruiterInboxUseCase inboxUseCase;

    public EmailInboxApiHandler(SyncRecruiterInboxUseCase inboxUseCase) {
        this.inboxUseCase = inboxUseCase;
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

        if (inboxUseCase == null) {
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

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<RecruiterEmailMessage> emails = inboxUseCase.getAllReceivedEmails();
        byte[] responseBytes = MAPPER.writeValueAsString(emails).getBytes(StandardCharsets.UTF_8);

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
            if (node == null) {
                sendError(exchange, 400);
                return;
            }

            String sender = node.has("sender")
                    ? node.get("sender").asText()
                    : (node.has("from") ? node.get("from").asText() : "");
            String recipient = node.has("recipient")
                    ? node.get("recipient").asText()
                    : (node.has("to") ? node.get("to").asText() : "");
            String subject = node.has("subject") ? node.get("subject").asText() : "";
            String bodyText = node.has("bodyText")
                    ? node.get("bodyText").asText()
                    : (node.has("body") ? node.get("body").asText() : "");

            RecruiterEmailMessage created = inboxUseCase.receiveIncomingEmail(sender, recipient, subject, bodyText);

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

    private void sendError(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }
}
