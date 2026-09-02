package com.watashi.adapters.in.web;

import com.sun.net.httpserver.HttpServer;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import com.watashi.core.ports.in.ManageFilterConfigUseCase;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
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

    public DashboardHttpServer(int port) {
        this(port, null, null, (ManageCandidateProfileUseCase) null, (GetJobsUseCase) null);
    }

    public DashboardHttpServer(int port, TrackJobApplicationUseCase trackUseCase) {
        this(
                port,
                null,
                null,
                (ManageCandidateProfileUseCase) null,
                (ManageFilterConfigUseCase) null,
                null,
                null,
                null,
                trackUseCase);
    }

    public DashboardHttpServer(int port, DispatchJobApplicationUseCase dispatchUseCase) {
        this(
                port,
                null,
                null,
                (ManageCandidateProfileUseCase) null,
                (ManageFilterConfigUseCase) null,
                null,
                null,
                null,
                null,
                dispatchUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                (ManageFilterConfigUseCase) null,
                getJobsUseCase,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                (ManageFilterConfigUseCase) null,
                null,
                ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                (ManageFilterConfigUseCase) null,
                getJobsUseCase,
                ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                toGetJobsUseCase(jobRepository),
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                toGetJobsUseCase(jobRepository),
                ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                ingestUseCase,
                coverLetterUseCase,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileRepository,
                filterRepository,
                jobRepository,
                getJobsUseCase,
                ingestUseCase,
                coverLetterUseCase,
                trackUseCase,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase,
            DispatchJobApplicationUseCase dispatchUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                ingestUseCase,
                coverLetterUseCase,
                trackUseCase,
                dispatchUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                toFilterUseCase(filterRepository),
                toGetJobsUseCase(jobRepository),
                ingestUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                ingestUseCase,
                coverLetterUseCase,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                filterUseCase,
                getJobsUseCase,
                ingestUseCase,
                null,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                filterUseCase,
                getJobsUseCase,
                ingestUseCase,
                coverLetterUseCase,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                filterUseCase,
                getJobsUseCase,
                ingestUseCase,
                coverLetterUseCase,
                trackUseCase,
                null);
    }

    public DashboardHttpServer(int port, SyncRecruiterInboxUseCase inboxUseCase, ManageCustomTagsUseCase tagsUseCase) {
        this(
                port,
                null,
                null,
                (ManageCandidateProfileUseCase) null,
                (ManageFilterConfigUseCase) null,
                null,
                null,
                null,
                null,
                null,
                inboxUseCase,
                tagsUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase,
            DispatchJobApplicationUseCase dispatchUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                profileUseCase,
                filterUseCase,
                getJobsUseCase,
                ingestUseCase,
                coverLetterUseCase,
                trackUseCase,
                dispatchUseCase,
                null,
                null);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterRepository,
            JobRepository jobRepository,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase,
            DispatchJobApplicationUseCase dispatchUseCase,
            SyncRecruiterInboxUseCase inboxUseCase,
            ManageCustomTagsUseCase tagsUseCase) {
        this(
                port,
                discoverUseCase,
                assessUseCase,
                toProfileUseCase(profileRepository),
                toFilterUseCase(filterRepository),
                getJobsUseCase != null ? getJobsUseCase : toGetJobsUseCase(jobRepository),
                ingestUseCase,
                coverLetterUseCase,
                trackUseCase,
                dispatchUseCase,
                inboxUseCase,
                tagsUseCase);
    }

    public DashboardHttpServer(
            int port,
            DiscoverJobsUseCase discoverUseCase,
            AssessJobCompatibilityUseCase assessUseCase,
            ManageCandidateProfileUseCase profileUseCase,
            ManageFilterConfigUseCase filterUseCase,
            GetJobsUseCase getJobsUseCase,
            IngestCandidateProfileUseCase ingestUseCase,
            GenerateCoverLetterUseCase coverLetterUseCase,
            TrackJobApplicationUseCase trackUseCase,
            DispatchJobApplicationUseCase dispatchUseCase,
            SyncRecruiterInboxUseCase inboxUseCase,
            ManageCustomTagsUseCase tagsUseCase) {
        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/", new IndexHtmlHandler());
            this.server.createContext(
                    "/api/jobs",
                    new JobApiHandler(discoverUseCase, assessUseCase, profileUseCase, filterUseCase, getJobsUseCase));
            this.server.createContext("/api/profile", new ProfileApiHandler(profileUseCase, ingestUseCase));
            this.server.createContext(
                    "/api/cover-letter",
                    new CoverLetterApiHandler(
                            coverLetterUseCase, assessUseCase, profileUseCase, filterUseCase, getJobsUseCase));
            JobApplicationApiHandler appHandler = new JobApplicationApiHandler(trackUseCase);
            this.server.createContext("/api/jobs/status", appHandler);
            this.server.createContext("/api/jobs/batch-status", appHandler);
            this.server.createContext("/api/jobs/history", appHandler);
            DispatcherApiHandler dispatcherHandler = new DispatcherApiHandler(dispatchUseCase);
            this.server.createContext("/api/applications/dispatch", dispatcherHandler);
            this.server.createContext("/api/applications/logs", dispatcherHandler);
            this.server.createContext("/api/credentials", dispatcherHandler);
            EmailInboxApiHandler inboxHandler = new EmailInboxApiHandler(inboxUseCase);
            this.server.createContext("/api/inbox", inboxHandler);
            this.server.createContext("/api/inbox/parse", inboxHandler);
            CustomTagsApiHandler tagsHandler = new CustomTagsApiHandler(tagsUseCase);
            this.server.createContext("/api/tags", tagsHandler);
            this.executor = Executors.newFixedThreadPool(4);
            this.server.setExecutor(this.executor);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to initialize Dashboard HTTP Server on port " + port + ": " + e.getMessage(), e);
        }
    }

    private static ManageCandidateProfileUseCase toProfileUseCase(CandidateProfileRepository profileRepository) {
        return profileRepository != null
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
                : null;
    }

    private static ManageFilterConfigUseCase toFilterUseCase(FilterConfigRepository filterRepository) {
        return filterRepository != null
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
