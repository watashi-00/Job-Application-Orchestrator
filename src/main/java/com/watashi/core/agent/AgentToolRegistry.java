package com.watashi.core.agent;

import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.discovery.SkillDictionary;
import com.watashi.core.domain.discovery.SkillExtractor;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchingEngine;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.FilterConfigRepository;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import com.watashi.core.ports.out.SkillDictionaryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AgentToolRegistry {

    private final JobRepository jobRepository;
    private final RecruiterEmailRepository emailRepository;
    private final CandidateProfileRepository profileRepository;
    private final FilterConfigRepository filterConfigRepository;
    private final SkillDictionaryRepository skillDictionaryRepository;
    private final DocumentChunker documentChunker;

    public AgentToolRegistry(
            JobRepository jobRepository,
            RecruiterEmailRepository emailRepository,
            CandidateProfileRepository profileRepository,
            FilterConfigRepository filterConfigRepository,
            SkillDictionaryRepository skillDictionaryRepository,
            DocumentChunker documentChunker) {
        this.jobRepository = jobRepository;
        this.emailRepository = emailRepository;
        this.profileRepository = profileRepository;
        this.filterConfigRepository = filterConfigRepository;
        this.skillDictionaryRepository = skillDictionaryRepository;
        this.documentChunker = documentChunker != null ? documentChunker : new DocumentChunker();
    }

    public AgentToolRegistry(
            JobRepository jobRepository,
            RecruiterEmailRepository emailRepository,
            CandidateProfileRepository profileRepository) {
        this(jobRepository, emailRepository, profileRepository, null, null, new DocumentChunker());
    }

    public List<JobOpportunity> searchJobs(String query) {
        if (jobRepository == null) {
            return List.of();
        }
        List<JobOpportunity> allJobs = jobRepository.findAll();
        if (query == null || query.isBlank()) {
            return allJobs;
        }
        String lowerQuery = query.toLowerCase().trim();
        return allJobs.stream()
                .filter(job -> (job.title() != null && job.title().toLowerCase().contains(lowerQuery))
                        || (job.company() != null && job.company().toLowerCase().contains(lowerQuery))
                        || (job.description() != null
                                && job.description().toLowerCase().contains(lowerQuery))
                        || (job.location() != null
                                && job.location().toLowerCase().contains(lowerQuery))
                        || job.hasSkillNamed(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<RecruiterEmailMessage> inspectRecruiterEmails(String company) {
        if (emailRepository == null) {
            return List.of();
        }
        List<RecruiterEmailMessage> allEmails = emailRepository.findAll();
        if (company == null || company.isBlank()) {
            return allEmails;
        }
        String lowerCompany = company.toLowerCase().trim();
        return allEmails.stream()
                .filter(email -> (email.matchedCompany() != null
                                && email.matchedCompany().toLowerCase().contains(lowerCompany))
                        || (email.sender() != null
                                && email.sender().toLowerCase().contains(lowerCompany))
                        || (email.subject() != null
                                && email.subject().toLowerCase().contains(lowerCompany))
                        || (email.bodyText() != null
                                && email.bodyText().toLowerCase().contains(lowerCompany)))
                .collect(Collectors.toList());
    }

    public MatchResult analyzeMatch(String jobId) {
        if (jobRepository == null || profileRepository == null || jobId == null || jobId.isBlank()) {
            return null;
        }
        Optional<JobOpportunity> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return null;
        }
        Optional<CandidateProfile> profileOpt = profileRepository.findDefault();
        if (profileOpt.isEmpty()) {
            return null;
        }
        FilterConfiguration filterConfig =
                filterConfigRepository != null ? filterConfigRepository.load() : FilterConfiguration.defaultConfig();
        MatchingEngine engine = new MatchingEngine();
        return engine.evaluate(jobOpt.get(), profileOpt.get(), filterConfig);
    }

    public Set<Skill> extractSkillsWithChunking(String text, ModelTier tier) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        ModelTier effectiveTier = tier != null ? tier : ModelTier.SMALL;
        List<String> chunks = documentChunker.chunkText(text, effectiveTier);
        SkillDictionary dictionary = skillDictionaryRepository != null
                ? skillDictionaryRepository.load()
                : SkillDictionary.defaultDictionary();
        Set<Skill> combinedSkills = new HashSet<>();
        for (String chunk : chunks) {
            Set<Skill> extracted = SkillExtractor.extractSkills(chunk, dictionary);
            combinedSkills.addAll(extracted);
        }
        return Set.copyOf(combinedSkills);
    }

    public Set<Skill> extractSkillsWithChunking(String text) {
        return extractSkillsWithChunking(text, ModelTier.SMALL);
    }
}
