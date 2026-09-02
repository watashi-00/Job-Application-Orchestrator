package com.watashi.runtime;

import com.watashi.adapters.in.email.MiniMxServer;
import com.watashi.adapters.in.web.DashboardHttpServer;
import com.watashi.adapters.out.ingestor.pdf.PdfCandidateIngestor;
import com.watashi.adapters.out.jobsource.arbeitnow.ArbeitnowJobSource;
import com.watashi.adapters.out.jobsource.jobicy.JobicyJobSource;
import com.watashi.adapters.out.jobsource.remotive.RemotiveJobSource;
import com.watashi.adapters.out.llm.OllamaLlmProvider;
import com.watashi.adapters.out.persistence.json.JsonAgentConfigRepository;
import com.watashi.adapters.out.persistence.json.JsonCandidateCredentialsRepository;
import com.watashi.adapters.out.persistence.json.JsonCandidateProfileRepository;
import com.watashi.adapters.out.persistence.json.JsonCustomTagRepository;
import com.watashi.adapters.out.persistence.json.JsonFilterConfigRepository;
import com.watashi.adapters.out.persistence.json.JsonJobApplicationRepository;
import com.watashi.adapters.out.persistence.json.JsonJobRepository;
import com.watashi.adapters.out.persistence.json.JsonRecruiterEmailRepository;
import com.watashi.adapters.out.persistence.json.JsonResumePdfStorageRepository;
import com.watashi.adapters.out.persistence.json.JsonSkillDictionaryRepository;
import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentToolRegistry;
import com.watashi.core.agent.DocumentChunker;
import com.watashi.core.domain.candidate.CandidatePreferences;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SalaryRange;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.discovery.SkillDictionary;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchingEngine;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.in.DispatchJobApplicationUseCase;
import com.watashi.core.ports.in.GenerateCoverLetterUseCase;
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.GetSystemLogsUseCase;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import com.watashi.core.ports.in.TrackJobApplicationUseCase;
import com.watashi.core.ports.out.AgentConfigRepository;
import com.watashi.core.ports.out.CandidateCredentialsRepository;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.CustomTagRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobApplicationRepository;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.JobSource;
import com.watashi.core.ports.out.LlmProviderPort;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import com.watashi.core.ports.out.ResumePdfStorageRepository;
import com.watashi.core.ports.out.SkillDictionaryRepository;
import com.watashi.core.service.DefaultAgentOrchestratorService;
import com.watashi.core.service.DefaultAssessJobCompatibilityService;
import com.watashi.core.service.DefaultCoverLetterService;
import com.watashi.core.service.DefaultDiscoverJobsService;
import com.watashi.core.service.DefaultDispatchJobApplicationService;
import com.watashi.core.service.DefaultGetSystemLogsService;
import com.watashi.core.service.DefaultIngestCandidateProfileService;
import com.watashi.core.service.DefaultManageCustomTagsService;
import com.watashi.core.service.DefaultSyncRecruiterInboxService;
import com.watashi.core.service.DefaultTrackJobApplicationService;
import com.watashi.infrastructure.http.HttpEngine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class Bootstrap {

    private Bootstrap() {}

    public static void start(String... args) {
        System.out.println("===============================================================");
        System.out.println("   JOB APPLICATION ORCHESTRATOR — LIVE DEMO & MATCHING TEST");
        System.out.println("===============================================================");
        System.out.println("CWD: " + new File(".").getAbsolutePath());

        HttpEngine httpEngine = HttpEngine.createDefault();
        JobSource remotiveSource = new RemotiveJobSource(httpEngine);
        JobSource arbeitnowSource = new ArbeitnowJobSource(httpEngine);
        JobSource jobicySource = new JobicyJobSource(httpEngine);

        // Persistent JSON repositories in ./data/
        JobRepository jobRepository = new JsonJobRepository();
        CandidateProfileRepository candidateRepository = new JsonCandidateProfileRepository();
        FilterConfigRepository filterConfigRepository = new JsonFilterConfigRepository();
        SkillDictionaryRepository dictionaryRepository = new JsonSkillDictionaryRepository();
        SkillDictionary skillDictionary = dictionaryRepository.load();
        dictionaryRepository.save(skillDictionary);

        ResumePdfStorageRepository pdfRepository = new JsonResumePdfStorageRepository();
        CandidateProfileIngestor pdfIngestor = new PdfCandidateIngestor();
        IngestCandidateProfileUseCase ingestUseCase =
                new DefaultIngestCandidateProfileService(pdfIngestor, candidateRepository, pdfRepository);

        CandidateProfile profile;
        if (candidateRepository.findDefault().isPresent() && pdfRepository.exists()) {
            System.out.println("\n--> Loaded existing Candidate Profile & Resume PDF from disk...");
            profile = candidateRepository.findDefault().get();
        } else {
            byte[] samplePdf = generateSampleResumePdf();
            CandidatePreferences preferences = new CandidatePreferences(
                    new SalaryRange(new BigDecimal("10000"), new BigDecimal("20000"), "USD"),
                    Set.of(WorkMode.REMOTE),
                    Set.of(SeniorityLevel.LEAD, SeniorityLevel.SENIOR, SeniorityLevel.MID),
                    Set.of("Remote"));

            System.out.println("\n--> Ingesting candidate resume PDF using Apache PDFBox & saving profile...");
            profile = ingestUseCase.ingestFromPdf(samplePdf, preferences);
        }

        System.out.println("\n[Ingested Candidate Profile]: " + profile.title());
        System.out.println("  Extracted Skills from PDF: "
                + profile.skills().stream().map(Skill::name).collect(Collectors.joining(", ")));
        System.out.println("  Target Seniority: " + profile.targetSeniorities());
        System.out.println("  Preferred Work Mode: " + profile.preferredWorkModes());

        MatchingEngine matchingEngine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(matchingEngine);

        DiscoverJobsUseCase discoverUseCase = new DefaultDiscoverJobsService(
                List.of(remotiveSource, arbeitnowSource, jobicySource), assessUseCase, jobRepository);

        FilterConfiguration filterConfig = filterConfigRepository.load();

        System.out.println("\n--> Fetching real remote jobs from APIs, evaluating & persisting...");
        List<MatchResult> results = discoverUseCase.discoverAndEvaluate(profile, filterConfig);

        System.out.println("\n===============================================================");
        System.out.println("  DISCOVERY & MATCHING RESULTS (Found " + results.size() + " jobs, persisted to disk)");
        System.out.println("===============================================================");

        int rank = 1;
        for (MatchResult result : results.stream().limit(10).toList()) {
            JobOpportunity job = jobRepository.findById(result.jobId()).orElse(null);
            if (job == null) continue;

            String statusIcon =
                    switch (result.status()) {
                        case RECOMMENDED -> "🟢 [RECOMMENDED]";
                        case CONDITIONAL -> "🟡 [CONDITIONAL]";
                        case REJECTED -> "🔴 [REJECTED]";
                    };

            System.out.printf(
                    "\n#%d %s - %.1f%% Score | %s at %s\n",
                    rank++, statusIcon, result.overallScore(), job.title(), job.company());
            System.out.println("   URL: " + job.sourceUrl());
            System.out.println("   Seniority: " + job.seniorityLevel() + " | Work Mode: " + job.workMode());

            String matchedNames =
                    result.matchedSkills().stream().map(Skill::name).collect(Collectors.joining(", "));
            String missingNames =
                    result.missingRequiredSkills().stream().map(Skill::name).collect(Collectors.joining(", "));

            System.out.println("   Matched Skills: " + (matchedNames.isEmpty() ? "None" : matchedNames));
            if (!missingNames.isEmpty()) {
                System.out.println("   Missing Skills: " + missingNames);
            }
            if (!result.conflicts().isEmpty()) {
                System.out.println("   Conflicts: " + String.join("; ", result.conflicts()));
            }
        }

        System.out.println("\n===============================================================");
        System.out.println("   Local Persistence Files Saved at:");
        System.out.println("   - " + Paths.get("data", "jobs.json").toAbsolutePath() + " ("
                + jobRepository.findAll().size() + " jobs)");
        System.out.println("   - " + Paths.get("data", "profile.json").toAbsolutePath());
        System.out.println("   - " + Paths.get("data", "filters.json").toAbsolutePath());
        System.out.println("   - " + Paths.get("data", "skills-dictionary.json").toAbsolutePath());
        System.out.println("===============================================================");

        JobApplicationRepository applicationRepository = new JsonJobApplicationRepository();
        TrackJobApplicationUseCase trackUseCase =
                new DefaultTrackJobApplicationService(applicationRepository, jobRepository);

        CandidateCredentialsRepository credentialsRepository = new JsonCandidateCredentialsRepository();
        DispatchJobApplicationUseCase dispatchUseCase =
                new DefaultDispatchJobApplicationService(jobRepository, applicationRepository, credentialsRepository);

        CustomTagRepository customTagRepository = new JsonCustomTagRepository();
        ManageCustomTagsUseCase tagsService = new DefaultManageCustomTagsService(customTagRepository);

        RecruiterEmailRepository emailRepository = new JsonRecruiterEmailRepository();
        SyncRecruiterInboxUseCase inboxService = new DefaultSyncRecruiterInboxService(emailRepository, jobRepository);

        MiniMxServer mxServer = new MiniMxServer(2525, inboxService);
        mxServer.start();
        System.out.println("📧 MiniMX SMTP Server live at smtp://localhost:2525");

        GetJobsUseCase getJobsUseCase = jobRepository::findAll;
        GenerateCoverLetterUseCase coverLetterService = new DefaultCoverLetterService();
        GetSystemLogsUseCase systemLogsService = new DefaultGetSystemLogsService(dispatchUseCase, inboxService);

        AgentConfigRepository agentConfigRepo = new JsonAgentConfigRepository();
        AgentConfig agentConfig = agentConfigRepo.loadConfig();
        LlmProviderPort llmProvider = new OllamaLlmProvider();
        AgentToolRegistry agentToolRegistry = new AgentToolRegistry(
                jobRepository,
                emailRepository,
                candidateRepository,
                filterConfigRepository,
                dictionaryRepository,
                new DocumentChunker());
        DefaultAgentOrchestratorService agentService =
                new DefaultAgentOrchestratorService(agentConfigRepo, llmProvider, agentToolRegistry);

        DashboardHttpServer server = new DashboardHttpServer(
                8080,
                discoverUseCase,
                assessUseCase,
                candidateRepository,
                filterConfigRepository,
                jobRepository,
                getJobsUseCase,
                ingestUseCase,
                coverLetterService,
                trackUseCase,
                dispatchUseCase,
                inboxService,
                tagsService,
                pdfRepository,
                systemLogsService,
                agentService,
                agentService);
        server.start();
        System.out.println("🌐 Web Dashboard live at http://localhost:8080");
    }

    private static byte[] generateSampleResumePdf() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                stream.newLineAtOffset(50, 700);
                stream.showText("Candidate Profile - Senior Backend Software Engineer");
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(0, -25);
                stream.showText(
                        "Backend Software Engineer specialized in Java 21, Spring Boot, distributed systems, Virtual Threads.");
                stream.newLineAtOffset(0, -20);
                stream.showText(
                        "Tecnologias: Java, Spring Boot, PostgreSQL, Docker, REST, WebSockets, Redis, Kafka, RabbitMQ, TypeScript, React, Linux, SQLite, Microservices, gRPC.");
                stream.endText();
            }
            doc.save(baos);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sample PDF: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }
}
