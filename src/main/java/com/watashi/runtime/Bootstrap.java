package com.watashi.runtime;

import com.watashi.adapters.in.web.DashboardHttpServer;
import com.watashi.adapters.out.ingestor.pdf.PdfCandidateIngestor;
import com.watashi.adapters.out.jobsource.arbeitnow.ArbeitnowJobSource;
import com.watashi.adapters.out.jobsource.jobicy.JobicyJobSource;
import com.watashi.adapters.out.jobsource.remotive.RemotiveJobSource;
import com.watashi.adapters.out.persistence.json.JsonCandidateProfileRepository;
import com.watashi.adapters.out.persistence.json.JsonFilterConfigRepository;
import com.watashi.adapters.out.persistence.json.JsonJobRepository;
import com.watashi.adapters.out.persistence.json.JsonSkillDictionaryRepository;
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
import com.watashi.core.ports.in.GetJobsUseCase;
import com.watashi.core.ports.in.IngestCandidateProfileUseCase;
import com.watashi.core.ports.out.CandidateProfileIngestor;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.JobSource;
import com.watashi.core.ports.out.SkillDictionaryRepository;
import com.watashi.core.service.DefaultAssessJobCompatibilityService;
import com.watashi.core.service.DefaultDiscoverJobsService;
import com.watashi.core.service.DefaultIngestCandidateProfileService;
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

        CandidateProfileIngestor pdfIngestor = new PdfCandidateIngestor();
        IngestCandidateProfileUseCase ingestUseCase =
                new DefaultIngestCandidateProfileService(pdfIngestor, candidateRepository);

        byte[] samplePdf = generateSampleResumePdf();
        CandidatePreferences preferences = new CandidatePreferences(
                new SalaryRange(new BigDecimal("10000"), new BigDecimal("20000"), "USD"),
                Set.of(WorkMode.REMOTE),
                Set.of(SeniorityLevel.LEAD, SeniorityLevel.SENIOR, SeniorityLevel.MID),
                Set.of("Remote"));

        System.out.println("\n--> Ingesting candidate resume PDF using Apache PDFBox & saving profile...");
        CandidateProfile profile = ingestUseCase.ingestFromPdf(samplePdf, preferences);

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

        GetJobsUseCase getJobsUseCase = jobRepository::findAll;
        DashboardHttpServer server = new DashboardHttpServer(
                8080,
                discoverUseCase,
                assessUseCase,
                candidateRepository,
                filterConfigRepository,
                jobRepository,
                getJobsUseCase,
                ingestUseCase);
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
                stream.showText("RODRIGO A. S AMARANTE - Backend Software Engineer");
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(0, -25);
                stream.showText(
                        "Engenheiro de Software Backend especializado em Java 21, Spring Boot, concorrência avançada, Virtual Threads.");
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
