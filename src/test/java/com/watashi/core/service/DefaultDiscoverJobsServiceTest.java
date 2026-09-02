package com.watashi.core.service;

import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.common.SkillCategory;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.discovery.JobQuery;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.domain.matching.FilterConfiguration;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.domain.matching.MatchingEngine;
import com.watashi.core.ports.in.AssessJobCompatibilityUseCase;
import com.watashi.core.ports.in.DiscoverJobsUseCase;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.JobSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultDiscoverJobsServiceTest extends TestCase {

    public void testDiscoverAndEvaluateJobs() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Java Dev",
                "Company",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);

        JobSource mockSource = new JobSource() {
            public String getSourceName() {
                return "MockSource";
            }

            public List<JobOpportunity> fetchJobs(JobQuery query) {
                return List.of(job);
            }
        };

        JobRepository mockRepo = new JobRepository() {
            private final Map<String, JobOpportunity> store = new HashMap<>();

            public void save(JobOpportunity j) {
                store.put(j.id(), j);
            }

            public List<JobOpportunity> findAll() {
                return new ArrayList<>(store.values());
            }

            public Optional<JobOpportunity> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }
        };

        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(engine);

        DiscoverJobsUseCase discoverUseCase =
                new DefaultDiscoverJobsService(List.of(mockSource), assessUseCase, mockRepo);

        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        List<MatchResult> results = discoverUseCase.discoverAndEvaluate(profile, FilterConfiguration.defaultConfig());

        assertEquals(1, results.size());
        assertEquals(100.0, results.get(0).overallScore(), 0.01);
        assertEquals(1, mockRepo.findAll().size());
    }

    public void testResilienceWhenSourceFails() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job = new JobOpportunity(
                "j1",
                "Java Dev",
                "Company",
                "Desc",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url",
                JobStatus.DISCOVERED);

        JobSource failingSource = new JobSource() {
            public String getSourceName() {
                return "FailingSource";
            }

            public List<JobOpportunity> fetchJobs(JobQuery query) {
                throw new RuntimeException("Network error");
            }
        };

        JobSource workingSource = new JobSource() {
            public String getSourceName() {
                return "WorkingSource";
            }

            public List<JobOpportunity> fetchJobs(JobQuery query) {
                return List.of(job);
            }
        };

        JobRepository mockRepo = new JobRepository() {
            private final Map<String, JobOpportunity> store = new HashMap<>();

            public void save(JobOpportunity j) {
                store.put(j.id(), j);
            }

            public List<JobOpportunity> findAll() {
                return new ArrayList<>(store.values());
            }

            public Optional<JobOpportunity> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }
        };

        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(engine);

        DiscoverJobsUseCase discoverUseCase =
                new DefaultDiscoverJobsService(List.of(failingSource, workingSource), assessUseCase, mockRepo);

        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());
        List<MatchResult> results = discoverUseCase.discoverAndEvaluate(profile, FilterConfiguration.defaultConfig());

        assertEquals(1, results.size());
        assertEquals(1, mockRepo.findAll().size());
    }

    public void testNullChecks() {
        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(engine);
        JobRepository mockRepo = new JobRepository() {
            public void save(JobOpportunity j) {}

            public List<JobOpportunity> findAll() {
                return List.of();
            }

            public Optional<JobOpportunity> findById(String id) {
                return Optional.empty();
            }
        };

        try {
            new DefaultDiscoverJobsService(null, assessUseCase, mockRepo);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            new DefaultDiscoverJobsService(List.of(), null, mockRepo);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            new DefaultDiscoverJobsService(List.of(), assessUseCase, null);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        DiscoverJobsUseCase service = new DefaultDiscoverJobsService(List.of(), assessUseCase, mockRepo);
        CandidateProfile profile = new CandidateProfile(
                "c1", "Dev", "Summary", Set.of(), Set.of(SeniorityLevel.MID), Set.of(WorkMode.REMOTE), null, Set.of());

        try {
            service.discoverAndEvaluate(null, FilterConfiguration.defaultConfig());
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }

        try {
            service.discoverAndEvaluate(profile, null);
            fail("Expected NPE");
        } catch (NullPointerException expected) {
        }
    }

    public void testDeduplicationAcrossMultipleSources() {
        Skill java = new Skill("Java", SkillCategory.LANGUAGES_FRAMEWORKS, 5);
        JobOpportunity job1 = new JobOpportunity(
                "j1",
                "Java Developer",
                "Acme Corp",
                "Short description",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url1",
                JobStatus.DISCOVERED);

        JobOpportunity job2 = new JobOpportunity(
                "j2",
                "Java Developer",
                "Acme Inc.",
                "Detailed long description of Java Developer role at Acme",
                Set.of(java),
                Set.of(),
                SeniorityLevel.MID,
                WorkMode.REMOTE,
                "Remote",
                null,
                "url2",
                JobStatus.DISCOVERED);

        JobSource source1 = new JobSource() {
            public String getSourceName() {
                return "Source1";
            }

            public List<JobOpportunity> fetchJobs(JobQuery query) {
                return List.of(job1);
            }
        };

        JobSource source2 = new JobSource() {
            public String getSourceName() {
                return "Source2";
            }

            public List<JobOpportunity> fetchJobs(JobQuery query) {
                return List.of(job2);
            }
        };

        JobRepository mockRepo = new JobRepository() {
            private final Map<String, JobOpportunity> store = new HashMap<>();

            public void save(JobOpportunity j) {
                store.put(j.id(), j);
            }

            public List<JobOpportunity> findAll() {
                return new ArrayList<>(store.values());
            }

            public Optional<JobOpportunity> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }
        };

        MatchingEngine engine = new MatchingEngine();
        AssessJobCompatibilityUseCase assessUseCase = new DefaultAssessJobCompatibilityService(engine);

        DiscoverJobsUseCase discoverUseCase =
                new DefaultDiscoverJobsService(List.of(source1, source2), assessUseCase, mockRepo);

        CandidateProfile profile = new CandidateProfile(
                "c1",
                "Dev",
                "Summary",
                Set.of(java),
                Set.of(SeniorityLevel.MID),
                Set.of(WorkMode.REMOTE),
                null,
                Set.of());

        List<MatchResult> results = discoverUseCase.discoverAndEvaluate(profile, FilterConfiguration.defaultConfig());

        assertEquals(1, results.size());
        assertEquals("j2", results.get(0).jobId());
        assertEquals(1, mockRepo.findAll().size());
        assertEquals("j2", mockRepo.findAll().get(0).id());
    }
}
