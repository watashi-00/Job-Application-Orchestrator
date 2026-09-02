package com.watashi.adapters.in.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            String body = response.body();

            assertTrue(body.contains("JOB APPLICATION ORCHESTRATOR"));
            assertTrue(body.contains("DASHBOARD"));
            assertTrue(body.contains("Upload Resume PDF"));
            assertTrue(body.contains("pdf-file-input"));
            assertTrue(body.contains("View Resume PDF"));
            assertTrue(body.contains("pdf-modal"));
            assertTrue(body.contains("Open in Gmail"));
            assertTrue(body.contains("updateProfileSeniorities"));
            assertTrue(body.contains("Generate Cover Letter"));
            assertTrue(body.contains("copyCoverLetter"));
            assertTrue(body.contains("APPLIED"));
            assertTrue(body.contains("IGNORED"));
            assertTrue(body.contains("batchApplySelected"));

            // Agent Chat Panel & Config Modal presence
            assertTrue(body.contains("agent-chat-panel"));
            assertTrue(body.contains("agent-config-modal"));

            // Emoji removal verification: ensure NO emoji characters exist in body
            Pattern emojiPattern = Pattern.compile("[\uD83C-\uD83E][\uDC00-\uDFFF]");
            Matcher matcher = emojiPattern.matcher(body);
            assertFalse("HTML should contain 0 emojis", matcher.find());
        } finally {
            server.stop();
        }
    }
}
