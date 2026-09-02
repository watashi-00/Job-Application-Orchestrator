package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpServer;
import com.watashi.core.ports.out.ResumePdfStorageRepository;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import junit.framework.TestCase;

public class ResumePdfApiHandlerTest extends TestCase {

    private HttpServer server;
    private ResumePdfStorageRepository mockRepository;
    private byte[] storedPdfBytes;
    private boolean pdfExists;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pdfExists = false;
        storedPdfBytes = null;

        mockRepository = new ResumePdfStorageRepository() {
            @Override
            public void savePdf(byte[] pdfBytes) {
                storedPdfBytes = pdfBytes;
                pdfExists = pdfBytes != null;
            }

            @Override
            public Optional<byte[]> loadPdf() {
                return pdfExists ? Optional.ofNullable(storedPdfBytes) : Optional.empty();
            }

            @Override
            public boolean exists() {
                return pdfExists;
            }
        };

        server = HttpServer.create(new InetSocketAddress(18095), 0);
        server.createContext("/api/profile/resume", new ResumePdfApiHandler(mockRepository));
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        super.tearDown();
    }

    public void testGetResumePdfNotFoundWhenMissing() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:18095/api/profile/resume"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    public void testGetResumePdfSuccessWhenPresent() throws Exception {
        byte[] pdfData = "%PDF-1.4 sample PDF document bytes".getBytes();
        mockRepository.savePdf(pdfData);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:18095/api/profile/resume"))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertEquals(
                "application/pdf", response.headers().firstValue("Content-Type").orElse(null));
        assertEquals(
                "inline; filename=\"resume.pdf\"",
                response.headers().firstValue("Content-Disposition").orElse(null));
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals(new String(pdfData), new String(response.body()));
    }

    public void testOptionsPreflight() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:18095/api/profile/resume"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals(
                "*",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertTrue(response.headers()
                .firstValue("Access-Control-Allow-Methods")
                .orElse("")
                .contains("GET"));
    }

    public void testMethodNotAllowedForPost() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:18095/api/profile/resume"))
                .POST(HttpRequest.BodyPublishers.ofString("test"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    public void testNullRepositoryReturns404() throws Exception {
        HttpServer altServer = HttpServer.create(new InetSocketAddress(18096), 0);
        altServer.createContext("/api/profile/resume", new ResumePdfApiHandler(null));
        altServer.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18096/api/profile/resume"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(404, response.statusCode());
        } finally {
            altServer.stop(0);
        }
    }
}
