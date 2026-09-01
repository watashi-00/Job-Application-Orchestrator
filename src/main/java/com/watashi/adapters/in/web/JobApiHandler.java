package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JobApiHandler implements HttpHandler {

    private final DiscoverJobsUseCase discoverUseCase;
    private final CandidateProfileRepository profileRepository;
    private final FilterConfigRepository filterRepository;
    private final JobRepository jobRepository;

    public JobApiHandler(
            DiscoverJobsUseCase discoverUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {
        this.discoverUseCase = discoverUseCase;
        this.profileRepository = profileRepository;
        this.filterRepository = filterRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method) && discoverUseCase != null && profileRepository != null) {
            CandidateProfile profile = profileRepository.findDefault().orElse(null);
            FilterConfiguration config =
                    filterRepository != null ? filterRepository.load() : FilterConfiguration.defaultConfig();
            if (profile != null) {
                discoverUseCase.discoverAndEvaluate(profile, config);
            }
        }

        List<JobOpportunity> jobs = jobRepository != null ? jobRepository.findAll() : List.of();
        byte[] responseBytes =
                JsonStorageUtils.createObjectMapper().writeValueAsString(jobs).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
