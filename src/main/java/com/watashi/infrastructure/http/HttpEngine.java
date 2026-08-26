package com.watashi.infrastructure.http;

import java.net.http.HttpClient;

public class HttpEngine {

    private static final HttpEngine INSTANCE =  new HttpEngine();
    private final HttpClient httpClient;

    private HttpEngine() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }// private constructor

    public static HttpEngine getInstance() {
        return INSTANCE;
    }

    /*
    The HTTP engine is stateless with respect to individual requests.
    Request-specific configuration is supplied through immutable request parameters,
    preventing shared mutable state between concurrent operations.
     */
    public static void fetch(RequestSpec requestSpec) {

    }

}
