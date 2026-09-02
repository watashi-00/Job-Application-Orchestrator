package com.watashi.adapters.in.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import junit.framework.TestCase;

public class IndexHtmlHandlerTest extends TestCase {

    public void testIndexHtmlResponse() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18081);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18081/"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("JOB APPLICATION ORCHESTRATOR"));
            assertTrue(response.body().contains("DASHBOARD"));
            assertTrue(response.body().contains("Upload Resume PDF"));
            assertTrue(response.body().contains("pdf-file-input"));
        } finally {
            server.stop();
        }
    }
}
