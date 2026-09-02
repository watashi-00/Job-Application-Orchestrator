package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.common.SystemActivityLogEntry;
import com.watashi.core.ports.in.GetSystemLogsUseCase;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ActivityLogsApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final GetSystemLogsUseCase logsUseCase;

    public ActivityLogsApiHandler(GetSystemLogsUseCase logsUseCase) {
        this.logsUseCase = logsUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        List<SystemActivityLogEntry> logs = logsUseCase != null ? logsUseCase.getUnifiedSystemLogs() : List.of();
        byte[] responseBytes = MAPPER.writeValueAsString(logs).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
