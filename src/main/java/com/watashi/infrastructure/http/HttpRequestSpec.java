package com.watashi.infrastructure.http;

import java.net.URI;
import java.util.Map;

// immutable object for Request
public record HttpRequestSpec(URI uri, Map<String, String> headers) implements RequestSpec {

    public HttpRequestSpec {
        headers = Map.copyOf(headers); // create an immutable Map
    }
}
