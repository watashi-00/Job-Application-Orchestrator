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

}
