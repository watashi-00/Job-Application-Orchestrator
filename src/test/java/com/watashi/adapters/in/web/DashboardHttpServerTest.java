package com.watashi.adapters.in.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import junit.framework.TestCase;

public class DashboardHttpServerTest extends TestCase {

    private DashboardHttpServer server;
    private int port = 18080;

    protected void setUp() throws Exception {
        server = new DashboardHttpServer(port, null, null, null, null, null);
        server.start();
    }

    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    public void testServerBinding() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/profile"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("{") || response.body().contains("null"));
    }
}
