package com.watashi.adapters.out.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.agent.AgentConfig;
import com.watashi.core.ports.out.LlmProviderPort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class OllamaLlmProvider implements LlmProviderPort {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaLlmProvider() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    }

    public OllamaLlmProvider(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = JsonStorageUtils.createObjectMapper();
    }

    @Override
    public String generate(String prompt, AgentConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        String baseUrl = config.ollamaUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String endpoint = baseUrl + "/api/generate";

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", config.modelName());
        requestPayload.put("prompt", prompt != null ? prompt : "");
        requestPayload.put("stream", false);

        try {
            String jsonRequestBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody, StandardCharsets.UTF_8));

            if (config.apiKey() != null && !config.apiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + config.apiKey());
            }

            HttpRequest httpRequest = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                        "Ollama API request failed with status code " + response.statusCode() + ": " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            if (rootNode.has("response")) {
                return rootNode.get("response").asText();
            } else {
                return response.body();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to communicate with Ollama endpoint " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Ollama response", e);
        }
    }
}
