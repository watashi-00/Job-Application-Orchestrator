package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.core.ports.out.ResumePdfStorageRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class ResumePdfApiHandler implements HttpHandler {

    private final ResumePdfStorageRepository pdfRepository;

    public ResumePdfApiHandler(ResumePdfStorageRepository pdfRepository) {
        this.pdfRepository = pdfRepository;
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

        if (pdfRepository == null || !pdfRepository.exists()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        Optional<byte[]> pdfOpt = pdfRepository.loadPdf();
        if (pdfOpt.isEmpty()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] pdfBytes = pdfOpt.get();
        exchange.getResponseHeaders().set("Content-Type", "application/pdf");
        exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"resume.pdf\"");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, pdfBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(pdfBytes);
        }
    }
}
