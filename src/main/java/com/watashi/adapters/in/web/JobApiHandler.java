package com.watashi.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.watashi.adapters.out.persistence.json.JsonStorageUtils;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageFilterConfigUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class JobApiHandler implements HttpHandler {

    private static final ObjectMapper MAPPER = JsonStorageUtils.createObjectMapper();

    private final DiscoverJobsUseCase discoverUseCase;
    private final ManageCandidateProfileUseCase profileUseCase;
    private final ManageFilterConfigUseCase filterUseCase;
    private final GetJobsUseCase getJobsUseCase;

    public JobApiHandler(
            DiscoverJobsUseCase discoverUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase) {
        this.discoverUseCase = discoverUseCase;
        this.profileUseCase = profileUseCase;
        this.filterUseCase = filterUseCase;
        this.getJobsUseCase = getJobsUseCase;
    }

    public JobApiHandler(
            DiscoverJobsUseCase discoverUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase) {
        this(discoverUseCase, profileUseCase, (ManageFilterConfigUseCase) null, getJobsUseCase);
    }

    public JobApiHandler(
            DiscoverJobsUseCase discoverUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {
        this(
                discoverUseCase,
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
                filterRepository != null
                        ? new ManageFilterConfigUseCase() {
                            @Override
                            public FilterConfiguration getConfig() {
                                return filterRepository.load();
                            }

                            @Override
                            public void updateConfig(FilterConfiguration config) {
                                filterRepository.save(config);
                            }
                        }
                        : null,
                jobRepository != null ? jobRepository::findAll : List::of);
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

        if ("POST".equalsIgnoreCase(method) && discoverUseCase != null && profileUseCase != null) {
            CandidateProfile profile = profileUseCase.getProfile().orElse(null);
            FilterConfiguration config =
                    filterUseCase != null ? filterUseCase.getConfig() : FilterConfiguration.defaultConfig();
            if (profile != null) {
                discoverUseCase.discoverAndEvaluate(profile, config);
            }
        }

        List<JobOpportunity> jobs = getJobsUseCase != null ? getJobsUseCase.getJobs() : List.of();
        byte[] responseBytes = MAPPER.writeValueAsString(jobs).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
