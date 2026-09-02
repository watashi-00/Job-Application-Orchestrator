package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpServer;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageFilterConfigUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardHttpServer {

    private final int port;
    private HttpServer server;
    private ExecutorService executor;

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase) {
        this(port, discoverUseCase, assessUseCase, (Object) profileUseCase, null, null, (Object) getJobsUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(port, discoverUseCase, assessUseCase, (Object) profileUseCase, null, null, (Object) ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {
        this(port, discoverUseCase, assessUseCase, (Object) profileRepository, filterRepository, jobRepository, (Object)
                toGetJobsUseCase(jobRepository));
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            Object profileSource,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            Object lastArg) {
        ManageCandidateProfileUseCase profileUseCase = null;
        if (profileSource instanceof ManageCandidateProfileUseCase m) {
            profileUseCase = m;
        } else if (profileSource instanceof CandidateProfileRepository repo) {
            profileUseCase = new ManageCandidateProfileUseCase() {
                @Override
                public Optional<CandidateProfile> getProfile() {
                    return repo.findDefault();
                }

                @Override
                public void updateProfile(CandidateProfile profile) {
                    repo.save(profile);
                }
            };
        }

        ManageFilterConfigUseCase filterUseCase = filterRepository != null
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
                : null;

        GetJobsUseCase getJobsUseCase = null;
        IngestCandidateProfileUseCase ingestUseCase = null;

        if (lastArg instanceof GetJobsUseCase g) {
            getJobsUseCase = g;
        } else if (lastArg instanceof IngestCandidateProfileUseCase i) {
            ingestUseCase = i;
        }

        if (getJobsUseCase == null && jobRepository != null) {
            getJobsUseCase = jobRepository::findAll;
        }

        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/", new IndexHtmlHandler());
            this.server.createContext(
                    "/api/jobs",
                    new JobApiHandler(discoverUseCase, assessUseCase, profileUseCase, filterUseCase, getJobsUseCase));
            this.server.createContext("/api/profile", new ProfileApiHandler(profileUseCase, ingestUseCase));
            this.executor = Executors.newFixedThreadPool(4);
            this.server.setExecutor(this.executor);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to initialize Dashboard HTTP Server on port " + port + ": " + e.getMessage(), e);
        }
    }

    private static GetJobsUseCase toGetJobsUseCase(JobRepository repository) {
        return repository != null ? repository::findAll : List::of;
    }

    public void start() {
        if (server != null) {
            server.start();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public int getPort() {
        return port;
    }
}
