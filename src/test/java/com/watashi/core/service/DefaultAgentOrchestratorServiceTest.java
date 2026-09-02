package com.watashi.core.service;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.agent.AgentToolRegistry;
import com.watashi.core.domain.candidate.CandidateProfile;
import com.watashi.core.domain.common.SeniorityLevel;
import com.watashi.core.domain.common.WorkMode;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.job.JobStatus;
import com.watashi.core.ports.in.AgentChatUseCase.AgentChatRequest;
import com.watashi.core.ports.in.AgentChatUseCase.AgentChatResponse;
import com.watashi.core.ports.out.AgentConfigRepository;
import com.watashi.core.ports.out.CandidateProfileRepository;
import com.watashi.core.ports.out.JobRepository;
import com.watashi.core.ports.out.LlmProviderPort;
import com.watashi.core.ports.out.RecruiterEmailRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;

public class DefaultAgentOrchestratorServiceTest extends TestCase {

    private DefaultAgentOrchestratorService service;
    private AgentConfig savedConfig;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        savedConfig = new AgentConfig("ollama", "http://localhost:11434", "llama3", ModelTier.SMALL, "", 0);

        AgentConfigRepository configRepo = new AgentConfigRepository() {
            @Override
            public AgentConfig loadConfig() {
                return savedConfig;
            }

            @Override
            public void saveConfig(AgentConfig config) {
                savedConfig = config;
            }
        };

        LlmProviderPort llmPort = (prompt, config) -> "LLM generated response for: " + prompt;

        JobRepository jobRepo = new JobRepository() {
            private final List<JobOpportunity> jobs = List.of(new JobOpportunity(
                    "job-1",
                    "Senior Java Developer",
                    "Acme Corp",
                    "Must know Java, Spring Boot, Docker",
                    Set.of(),
                    Set.of(),
                    SeniorityLevel.SENIOR,
                    WorkMode.REMOTE,
                    "Remote",
                    null,
                    "http://example.com",
                    JobStatus.DISCOVERED));

            @Override
            public void save(JobOpportunity job) {}

            @Override
            public List<JobOpportunity> findAll() {
                return jobs;
            }

            @Override
            public Optional<JobOpportunity> findById(String id) {
                return jobs.stream().filter(j -> j.id().equals(id)).findFirst();
            }
        };

        RecruiterEmailRepository emailRepo = new RecruiterEmailRepository() {
            private final List<RecruiterEmailMessage> emails = List.of(new RecruiterEmailMessage(
                    "e1",
                    "recruiter@acme.com",
                    "user@watashi.com",
                    "Interview Invitation",
                    "We would like to schedule an interview for Acme Corp",
                    null,
                    "job-1",
                    "Acme Corp",
                    JobStatus.INTERVIEWING));

            @Override
            public void save(RecruiterEmailMessage message) {}

            @Override
            public List<RecruiterEmailMessage> findAll() {
                return emails;
            }

            @Override
            public Optional<RecruiterEmailMessage> findById(String id) {
                return emails.stream().filter(e -> e.id().equals(id)).findFirst();
            }
        };

        CandidateProfileRepository profileRepo = new CandidateProfileRepository() {
            private CandidateProfile profile = new CandidateProfile(
                    "p1",
                    "Senior Java Developer",
                    "experienced engineer",
                    Set.of(),
                    Set.of(SeniorityLevel.SENIOR),
                    Set.of(WorkMode.REMOTE),
                    null,
                    Set.of());

            @Override
            public Optional<CandidateProfile> findDefault() {
                return Optional.of(profile);
            }

            @Override
            public void save(CandidateProfile profile) {
                this.profile = profile;
            }
        };

        AgentToolRegistry toolRegistry = new AgentToolRegistry(jobRepo, emailRepo, profileRepo);

        service = new DefaultAgentOrchestratorService(configRepo, llmPort, toolRegistry);
    }

    public void testGetAndUpdateAgentConfig() {
        AgentConfig config = service.getAgentConfig();
        assertEquals("ollama", config.provider());

        AgentConfig updated =
                new AgentConfig("ollama", "http://localhost:11434", "mistral", ModelTier.MEDIUM, "key123", 0);
        service.updateAgentConfig(updated);

        assertEquals("mistral", service.getAgentConfig().modelName());
        assertEquals(ModelTier.MEDIUM, service.getAgentConfig().modelTier());
    }

    public void testChatSimpleMessage() {
        String response = service.chat("Tell me a joke");
        assertNotNull(response);
        assertTrue(response.contains("LLM generated response"));
    }

    public void testChatWithEmailToolExecution() {
        AgentChatRequest request = new AgentChatRequest("Summarize active interview invites for Acme Corp");
        AgentChatResponse response = service.chat(request);

        assertNotNull(response.response());
        assertFalse(response.toolsExecuted().isEmpty());
        assertTrue(response.toolsExecuted().stream().anyMatch(t -> t.contains("inspectRecruiterEmails")));
    }

    public void testChatWithJobSearchToolExecution() {
        AgentChatRequest request = new AgentChatRequest("Search jobs for Java Developer");
        AgentChatResponse response = service.chat(request);

        assertNotNull(response.response());
        assertFalse(response.toolsExecuted().isEmpty());
        assertTrue(response.toolsExecuted().stream().anyMatch(t -> t.contains("searchJobs")));
    }
}
