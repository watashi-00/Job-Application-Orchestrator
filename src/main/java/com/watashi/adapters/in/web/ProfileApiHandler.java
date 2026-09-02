package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ProfileApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final ManageCandidateProfileUseCase profileUseCase;
    private final IngestCandidateProfileUseCase ingestUseCase;

    public ProfileApiHandler(
            ManageCandidateProfileUseCase profileUseCase, IngestCandidateProfileUseCase ingestUseCase) {
        this.profileUseCase = profileUseCase;
        this.ingestUseCase = ingestUseCase;
    }

    public ProfileApiHandler(ManageCandidateProfileUseCase profileUseCase) {
        this(profileUseCase, null);
    }

    public ProfileApiHandler(CandidateProfileRepository profileRepository) {
        this(
                profileRepository != null
                        ? new ManageCandidateProfileUseCase() {
                            @Override
                            public Optional<CandidateProfile> getProfile() {
                                return profileRepository.findDefault();
                            }

                            @Override
                            public void updateProfile(CandidateProfile profile) {
                                profileRepository.save(profile);
                            }
                        }
                        : null,
                null);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            byte[] pdfBytes = exchange.getRequestBody().readAllBytes();
            if (pdfBytes.length > 0 && ingestUseCase != null) {
                CandidateProfile updatedProfile = ingestUseCase.ingestFromPdf(pdfBytes, null);
                if (updatedProfile != null) {
                    if (profileUseCase != null) {
                        profileUseCase.updateProfile(updatedProfile);
                    }
                    byte[] responseBytes =
                            MAPPER.writeValueAsString(updatedProfile).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    return;
                }
            }
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        CandidateProfile profile =
                profileUseCase != null ? profileUseCase.getProfile().orElse(null) : null;

        if (profile == null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] responseBytes = MAPPER.writeValueAsString(profile).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
