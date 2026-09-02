package com.watashi.adapters.in.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ProfileApiHandler implements HttpHandler {

    public static final int MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024;

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
        String path = exchange.getRequestURI().getPath();

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
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if (path != null && path.endsWith("/update")) {
                handleProfileUpdate(exchange);
                return;
            }

            if (ingestUseCase == null) {
                exchange.sendResponseHeaders(501, -1);
                exchange.close();
                return;
            }

            byte[] pdfBytes = exchange.getRequestBody().readAllBytes();
            if (pdfBytes.length == 0 || pdfBytes.length > MAX_PDF_SIZE_BYTES) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            try {
                CandidateProfile existing =
                        profileUseCase != null ? profileUseCase.getProfile().orElse(null) : null;
                com.watashi.core.domain.candidate.CandidatePreferences prefs = null;
                if (existing != null) {
                    prefs = new com.watashi.core.domain.candidate.CandidatePreferences(
                            existing.desiredSalary(),
                            existing.preferredWorkModes(),
                            existing.targetSeniorities(),
                            existing.preferredLocations());
                }
                CandidateProfile updatedProfile = ingestUseCase.ingestFromPdf(pdfBytes, prefs);
                if (updatedProfile != null) {
                    if (profileUseCase != null) {
                        profileUseCase.updateProfile(updatedProfile);
                    }
                    byte[] responseBytes =
                            MAPPER.writeValueAsString(updatedProfile).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    return;
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

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

    private void handleProfileUpdate(HttpExchange exchange) throws IOException {
        if (profileUseCase == null) {
            exchange.sendResponseHeaders(501, -1);
            exchange.close();
            return;
        }

        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        try {
            JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.isObject()) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            CandidateProfile current = profileUseCase.getProfile().orElse(null);

            String id = node.has("id") ? node.get("id").asText() : (current != null ? current.id() : "c1");
            String title = node.has("title") ? node.get("title").asText() : (current != null ? current.title() : "");
            String summary =
                    node.has("summary") ? node.get("summary").asText() : (current != null ? current.summary() : "");

            Set<Skill> skills;
            if (node.has("skills")) {
                skills = new HashSet<>();
                JsonNode skillsNode = node.get("skills");
                if (skillsNode.isArray()) {
                    for (JsonNode skNode : skillsNode) {
                        if (skNode.isTextual()) {
                            skills.add(new Skill(skNode.asText(), null, 0));
                        } else if (skNode.isObject()) {
                            skills.add(MAPPER.convertValue(skNode, Skill.class));
                        }
                    }
                }
            } else {
                skills = current != null ? current.skills() : Set.of();
            }

            Set<SeniorityLevel> targetSeniorities;
            if (node.has("targetSeniorities")) {
                targetSeniorities = new HashSet<>();
                JsonNode senNode = node.get("targetSeniorities");
                if (senNode.isArray()) {
                    for (JsonNode s : senNode) {
                        try {
                            targetSeniorities.add(
                                    SeniorityLevel.valueOf(s.asText().toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } else {
                targetSeniorities = current != null ? current.targetSeniorities() : Set.of();
            }

            Set<WorkMode> preferredWorkModes;
            JsonNode wmNode = node.has("preferredWorkModes")
                    ? node.get("preferredWorkModes")
                    : (node.has("workModes") ? node.get("workModes") : null);
            if (wmNode != null) {
                preferredWorkModes = new HashSet<>();
                if (wmNode.isArray()) {
                    for (JsonNode w : wmNode) {
                        try {
                            preferredWorkModes.add(WorkMode.valueOf(w.asText().toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } else {
                preferredWorkModes = current != null ? current.preferredWorkModes() : Set.of();
            }

            SalaryRange desiredSalary = node.has("desiredSalary")
                    ? MAPPER.convertValue(node.get("desiredSalary"), SalaryRange.class)
                    : (current != null ? current.desiredSalary() : null);

            Set<String> preferredLocations = node.has("preferredLocations")
                    ? MAPPER.convertValue(node.get("preferredLocations"), new TypeReference<Set<String>>() {})
                    : (current != null ? current.preferredLocations() : Set.of());

            CandidateProfile updated = new CandidateProfile(
                    id,
                    title,
                    summary,
                    skills,
                    targetSeniorities,
                    preferredWorkModes,
                    desiredSalary,
                    preferredLocations);

            profileUseCase.updateProfile(updated);

            byte[] responseBytes = MAPPER.writeValueAsString(updated).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        }
    }
}
