package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpServer;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class DashboardHttpServer {

    private final int port;
    private HttpServer server;

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {
        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext(
                    "/api/jobs",
                    new JobApiHandler(discoverUseCase, profileRepository, filterRepository, jobRepository));
            this.server.createContext("/api/profile", new ProfileApiHandler(profileRepository));
            this.server.setExecutor(Executors.newFixedThreadPool(4));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to initialize Dashboard HTTP Server on port " + port + ": " + e.getMessage(), e);
        }
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
    }

    public int getPort() {
        return port;
    }
}
