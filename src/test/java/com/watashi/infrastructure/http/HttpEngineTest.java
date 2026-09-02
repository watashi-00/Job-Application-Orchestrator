package com.watashi.infrastructure.http;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

public class HttpEngineTest extends TestCase {

    private HttpEngine engine;

    @Override
    protected void setUp() {
        engine = HttpEngine.getInstance();
    }

    public void testFetchReturnsSuccessFullResponse() throws Exception {
        var req = new HttpRequestSpec(URI.create("https://example.com"), Map.of("Accept", "text/html"));

        CompletableFuture<HttpResponse<String>> future = engine.fetch(req);

        HttpResponse<String> response = future.get(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        assertFalse(response.body().isEmpty());
    }

    public void testFetchIsAsynchronous() {
        var request = new HttpRequestSpec(URI.create("https://example.com"), Map.of());

        CompletableFuture<HttpResponse<String>> future = engine.fetch(request);

        assertNotNull(future);
        assertFalse(future.isCancelled());
    }

    public void testDifferentRequestsCanRunConcurrently() throws Exception {
        var requestA = new HttpRequestSpec(URI.create("https://example.com"), Map.of("X-Test-Request", "A"));

        var requestB = new HttpRequestSpec(URI.create("https://example.com"), Map.of("X-Test-Request", "B"));

        var futureA = engine.fetch(requestA);
        var futureB = engine.fetch(requestB);

        var responseA = futureA.get(5, TimeUnit.SECONDS);
        var responseB = futureB.get(5, TimeUnit.SECONDS);

        assertNotNull(responseA);
        assertNotNull(responseB);

        assertEquals(200, responseA.statusCode());
        assertEquals(200, responseB.statusCode());
    }

    public void testRequestSpecHeadersAreIndependent() {
        var headersA = Map.of("Authorization", "Bearer A");
        var headersB = Map.of("Authorization", "Bearer B");

        var requestA = new HttpRequestSpec(URI.create("https://example.com"), headersA);

        var requestB = new HttpRequestSpec(URI.create("https://example.com"), headersB);

        assertEquals("Bearer A", requestA.headers().get("Authorization"));
        assertEquals("Bearer B", requestB.headers().get("Authorization"));
    }
}
