package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.JobSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DefaultDiscoverJobsService implements DiscoverJobsUseCase {

    private final List<JobSource> sources;
    private final AssessJobCompatibilityUseCase assessUseCase;
    private final JobRepository repository;

    public DefaultDiscoverJobsService(
            List<JobSource> sources, AssessJobCompatibilityUseCase assessUseCase, JobRepository repository) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources cannot be null"));
        this.assessUseCase = Objects.requireNonNull(assessUseCase, "assessUseCase cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Override
    public List<MatchResult> discoverAndEvaluate(CandidateProfile profile, FilterConfiguration config) {
        Objects.requireNonNull(profile, "profile cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        JobQuery query = JobQuery.ofSoftwareDev();
        List<JobOpportunity> fetchedJobs = new ArrayList<>();

        for (JobSource source : sources) {
            if (source == null) {
                continue;
            }
            try {
                List<JobOpportunity> jobs = source.fetchJobs(query);
                if (jobs != null) {
                    for (JobOpportunity job : jobs) {
                        if (job != null) {
                            repository.save(job);
                            fetchedJobs.add(job);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore exception to ensure failure in one source does not crash discovery process
            }
        }

        List<MatchResult> results = new ArrayList<>(assessUseCase.evaluateAll(fetchedJobs, profile, config));
        results.sort(Comparator.comparingDouble(MatchResult::overallScore).reversed());
        return List.copyOf(results);
    }
}
