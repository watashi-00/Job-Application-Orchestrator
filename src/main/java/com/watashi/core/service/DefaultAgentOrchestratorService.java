package com.watashi.core.service;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentToolRegistry;
import com.watashi.core.domain.common.Skill;
import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.domain.job.JobOpportunity;
import com.watashi.core.domain.matching.MatchResult;
import com.watashi.core.ports.in.AgentChatUseCase;
import com.watashi.core.ports.in.ManageAgentConfigUseCase;
import com.watashi.core.ports.out.AgentConfigRepository;
import com.watashi.core.ports.out.LlmProviderPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultAgentOrchestratorService implements AgentChatUseCase, ManageAgentConfigUseCase {

    private final AgentConfigRepository agentConfigRepository;
    private final LlmProviderPort llmProviderPort;
    private final AgentToolRegistry agentToolRegistry;

    public DefaultAgentOrchestratorService(
            AgentConfigRepository agentConfigRepository,
            LlmProviderPort llmProviderPort,
            AgentToolRegistry agentToolRegistry) {
        this.agentConfigRepository =
                Objects.requireNonNull(agentConfigRepository, "agentConfigRepository cannot be null");
        this.llmProviderPort = Objects.requireNonNull(llmProviderPort, "llmProviderPort cannot be null");
        this.agentToolRegistry = agentToolRegistry;
    }

    @Override
    public AgentConfig getAgentConfig() {
        AgentConfig config = agentConfigRepository.loadConfig();
        return config != null ? config : AgentConfig.defaultConfig();
    }

    @Override
    public void updateAgentConfig(AgentConfig config) {
        if (config != null) {
            agentConfigRepository.saveConfig(config);
        }
    }

    @Override
    public String chat(String message) {
        AgentChatResponse response = chat(new AgentChatRequest(message));
        return response.response();
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return new AgentChatResponse("Please provide a prompt message.");
        }

        AgentConfig config = request.configOverride() != null ? request.configOverride() : getAgentConfig();
        List<String> toolsExecuted = new ArrayList<>();
        StringBuilder toolContext = new StringBuilder();

        String prompt = request.message();
        String lowerPrompt = prompt.toLowerCase();

        // 1. Email inspection tool execution
        if (lowerPrompt.contains("email")
                || lowerPrompt.contains("inbox")
                || lowerPrompt.contains("invite")
                || lowerPrompt.contains("interview")
                || lowerPrompt.contains("recruiter")) {
            if (agentToolRegistry != null) {
                String companyQuery = extractCompanyQuery(prompt);
                List<RecruiterEmailMessage> emails = agentToolRegistry.inspectRecruiterEmails(companyQuery);
                toolsExecuted.add("inspectRecruiterEmails(" + (companyQuery != null ? companyQuery : "") + ")");
                toolContext
                        .append("Recruiter Emails Found (")
                        .append(emails.size())
                        .append("):\n");
                for (RecruiterEmailMessage email : emails) {
                    toolContext
                            .append("- From: ")
                            .append(email.sender())
                            .append(" | Subject: ")
                            .append(email.subject())
                            .append(" | Status: ")
                            .append(email.detectedStatus())
                            .append("\n");
                }
            }
        }

        // 2. Job search tool execution
        if (lowerPrompt.contains("search")
                || lowerPrompt.contains("job")
                || lowerPrompt.contains("vacanc")
                || lowerPrompt.contains("find")) {
            if (agentToolRegistry != null) {
                List<JobOpportunity> jobs = agentToolRegistry.searchJobs(prompt);
                toolsExecuted.add("searchJobs");
                toolContext.append("Matching Jobs Found (").append(jobs.size()).append("):\n");
                for (JobOpportunity job : jobs) {
                    toolContext
                            .append("- [")
                            .append(job.id())
                            .append("] ")
                            .append(job.title())
                            .append(" at ")
                            .append(job.company())
                            .append(" (")
                            .append(job.location())
                            .append(")\n");
                }
            }
        }

        // 3. Match analysis tool execution
        if (lowerPrompt.contains("match") || lowerPrompt.contains("compatib") || lowerPrompt.contains("analyze")) {
            if (agentToolRegistry != null) {
                String jobId = extractJobId(prompt);
                if (jobId != null) {
                    MatchResult result = agentToolRegistry.analyzeMatch(jobId);
                    toolsExecuted.add("analyzeMatch(" + jobId + ")");
                    if (result != null) {
                        toolContext
                                .append("Match Result for ")
                                .append(jobId)
                                .append(": Overall Score ")
                                .append(result.overallScore())
                                .append(", Status ")
                                .append(result.status())
                                .append("\n");
                    }
                }
            }
        }

        // 4. Skill extraction tool execution
        if (lowerPrompt.contains("extract") || lowerPrompt.contains("chunk") || lowerPrompt.contains("skill")) {
            if (agentToolRegistry != null) {
                Set<Skill> skills = agentToolRegistry.extractSkillsWithChunking(prompt, config.modelTier());
                toolsExecuted.add("extractSkillsWithChunking(" + config.modelTier() + ")");
                toolContext.append("Extracted Skills (").append(skills.size()).append("): ");
                for (Skill s : skills) {
                    toolContext.append(s.name()).append(", ");
                }
                toolContext.append("\n");
            }
        }

        StringBuilder fullPromptBuilder = new StringBuilder();
        if (request.context() != null && !request.context().isBlank()) {
            fullPromptBuilder.append("Context:\n").append(request.context()).append("\n\n");
        }
        if (toolContext.length() > 0) {
            fullPromptBuilder.append("Tool Observations:\n").append(toolContext).append("\n");
        }
        fullPromptBuilder.append("User Request: ").append(prompt);

        String llmOutput = llmProviderPort.generate(fullPromptBuilder.toString(), config);
        if (llmOutput == null || llmOutput.isBlank()) {
            llmOutput = toolContext.length() > 0
                    ? "Agent processed tools successfully:\n" + toolContext
                    : "No response generated by LLM provider.";
        }

        return new AgentChatResponse(llmOutput, toolsExecuted);
    }

    private String extractCompanyQuery(String prompt) {
        if (prompt.contains("for ")) {
            String sub = prompt.substring(prompt.indexOf("for ") + 4).trim();
            if (!sub.isBlank()) {
                return sub.split("\\s+")[0];
            }
        }
        return "";
    }

    private String extractJobId(String prompt) {
        for (String token : prompt.split("\\s+")) {
            if (token.startsWith("job-") || token.matches("^[a-f0-9\\-]{8,}$")) {
                return token;
            }
        }
        return null;
    }
}
