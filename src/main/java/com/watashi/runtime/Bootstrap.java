package com.watashi.runtime;

import com.watashi.adapters.out.jobsource.remotive.RemotiveJobSource;
import com.watashi.adapters.out.persistence.InMemoryJobRepository;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.*;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.*;
import com.watashi.core.ports.in.*;
import com.watashi.core.ports.out.*;
import com.watashi.core.service.*;
import com.watashi.infrastructure.http.HttpEngine;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Bootstrap {

    private Bootstrap() {}

    public static void start(String... args) {
        System.out.println("===============================================================");
        System.out.println("   JOB APPLICATION ORCHESTRATOR — LIVE DEMO & MATCHING TEST");
        System.out.println("===============================================================");

        HttpEngine httpEngine = HttpEngine.createDefault();
        JobSource remotiveSource = new RemotiveJobSource(httpEngine);
        JobRepository jobRepository = new InMemoryJobRepository();

        MatchingEngine matchingEngine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(matchingEngine);

        DiscoverJobsUseCase discoverUseCase =
                new DefaultDiscoverJobsService(List.of(remotiveSource), assessUseCase, jobRepository);

        // Define a candidate profile
        CandidateProfile sampleProfile = new CandidateProfile(
                "cand-demo",
                "Senior Backend Engineer",
                "Experienced Java & Cloud Engineer",
                Set.of(
                        new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5),
                        new Skill("Spring Boot", SkillCategory.LANGUAGES_FRAMEWORKS, 4),
                        new Skill("Docker", SkillCategory.DEVOPS_CLOUD, 3),
                        new Skill("PostgreSQL", SkillCategory.DATABASE, 4),
                        new Skill("REST", SkillCategory.ARCHITECTURE_DESIGN, 5)),
                Set.of(SeniorityLevel.SENIOR, SeniorityLevel.LEAD),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of("Remote"));

        System.out.println("\n[Candidate Profile]: " + sampleProfile.title());
        System.out.println(
                "  Skills: " + sampleProfile.skills().stream().map(Skill::name).collect(Collectors.joining(", ")));
        System.out.println("  Target Seniority: " + sampleProfile.targetSeniorities());
        System.out.println("  Preferred Work Mode: " + sampleProfile.preferredWorkModes());

        System.out.println("\n--> Fetching real remote jobs from Remotive API & evaluating compatibility...");

        List<MatchResult> results =
                discoverUseCase.discoverAndEvaluate(sampleProfile, FilterConfiguration.defaultConfig());

        System.out.println("\n===============================================================");
        System.out.println("  DISCOVERY & MATCHING RESULTS (Found " + results.size() + " jobs)");
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
        System.out.println("   Demo Completed Successfully.");
        System.out.println("===============================================================");
    }
}
