package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProfileApiHandler implements HttpHandler {

    private final CandidateProfileRepository profileRepository;

    public ProfileApiHandler(CandidateProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CandidateProfile profile =
                profileRepository != null ? profileRepository.findDefault().orElse(null) : null;
        byte[] responseBytes = JsonStorageUtils.createObjectMapper()
                .writeValueAsString(profile)
                .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
