package com.watashi.infrastructure.http;

import java.net.URI;
import java.util.Map;

public interface RequestSpec {
    URI uri();
    Map<String, String> headers();
}
