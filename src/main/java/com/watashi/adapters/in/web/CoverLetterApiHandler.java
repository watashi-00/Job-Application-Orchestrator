package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.CoverLetterResult;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageFilterConfigUseCase;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class CoverLetterApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final GenerateCoverLetterUseCase coverLetterUseCase;
    private final AssessJobCompatibilityUseCase assessUseCase;
    private final ManageCandidateProfileUseCase profileUseCase;
    private final ManageFilterConfigUseCase filterUseCase;
    private final GetJobsUseCase getJobsUseCase;
    private final JobRepository jobRepository;

    public CoverLetterApiHandler(
            GenerateCoverLetterUseCase coverLetterUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            JobRepository jobRepository) {
        this.coverLetterUseCase = coverLetterUseCase;
        this.assessUseCase = assessUseCase;
        this.profileUseCase = profileUseCase;
        this.filterUseCase = filterUseCase;
        this.getJobsUseCase = getJobsUseCase;
        this.jobRepository = jobRepository;
    }

    public CoverLetterApiHandler(
            GenerateCoverLetterUseCase coverLetterUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase) {
        this(coverLetterUseCase, assessUseCase, profileUseCase, filterUseCase, getJobsUseCase, null);
    }

    public CoverLetterApiHandler(
            GenerateCoverLetterUseCase coverLetterUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase) {
        this(coverLetterUseCase, assessUseCase, profileUseCase, null, getJobsUseCase, null);
    }

    public CoverLetterApiHandler(
            GenerateCoverLetterUseCase coverLetterUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            JobRepository jobRepository) {
        this(
                coverLetterUseCase,
                assessUseCase,
                profileUseCase,
                filterUseCase,
                jobRepository != null ? jobRepository::findAll : null,
                jobRepository);
    }

    public CoverLetterApiHandler(
            GenerateCoverLetterUseCase coverLetterUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            JobRepository jobRepository) {
        this(
                coverLetterUseCase,
                assessUseCase,
                profileUseCase,
                null,
                jobRepository != null ? jobRepository::findAll : null,
                jobRepository);
    }

    public CoverLetterApiHandler(GenerateCoverLetterUseCase coverLetterUseCase) {
        this(coverLetterUseCase, null, null, null, (GetJobsUseCase) null, null);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String jobId = extractQueryParam(exchange.getRequestURI().getRawQuery(), "jobId");
        if (jobId == null || jobId.isBlank()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
            return;
        }

        Optional<JobOpportunity> jobOpt = findJob(jobId);
        if (jobOpt.isEmpty()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(404, -1);
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

        JobOpportunity job = jobOpt.get();
        FilterConfiguration config =
                filterUseCase != null ? filterUseCase.getConfig() : FilterConfiguration.defaultConfig();
        MatchResult match = assessUseCase != null ? assessUseCase.evaluate(job, profile, config) : null;

        if (coverLetterUseCase == null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }

        CoverLetterResult result = coverLetterUseCase.generateCoverLetter(job, profile, match);

        byte[] responseBytes = MAPPER.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private Optional<JobOpportunity> findJob(String jobId) {
        if (jobRepository != null) {
            Optional<JobOpportunity> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                return jobOpt;
            }
        }
        if (getJobsUseCase != null) {
            return getJobsUseCase.getJobs().stream()
                    .filter(j -> j != null && jobId.equals(j.id()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private String extractQueryParam(String query, String paramName) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length > 0 && kv[0].equalsIgnoreCase(paramName)) {
                return kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }
}
