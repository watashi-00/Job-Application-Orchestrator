package com.watashi.infrastructure.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpEngine {
    private static final HttpEngine INSTANCE = new HttpEngine(
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    private final HttpClient httpClient;

    protected HttpEngine() {
        this.httpClient = null;
    }

    private HttpEngine(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static HttpEngine getInstance() {
        return INSTANCE;
    }

    public static HttpEngine createDefault() {
        return INSTANCE;
    }

    /*
    The HTTP engine is stateless with respect to individual requests.
    Request-specific configuration is supplied through immutable request parameters,
    preventing shared mutable state between concurrent operations.
     */
    public CompletableFuture<HttpResponse<String>> fetch(RequestSpec requestSpec) {
        return sendAsync(requestSpec);
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(RequestSpec requestSpec) {
        var request = buildRequest(requestSpec);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildRequest(RequestSpec requestSpec) {
        var builder = HttpRequest.newBuilder(requestSpec.uri());
        requestSpec.headers().forEach(builder::setHeader);
        return builder.GET().build();
    }
}
