package com.watashi.adapters.in.cli;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.agent.DocumentChunker;
import com.watashi.core.ports.in.AgentChatUseCase;
import com.watashi.core.ports.in.ManageAgentConfigUseCase;
import java.util.List;

public class AgentCliRunner {

    private final AgentChatUseCase agentChatUseCase;
    private final ManageAgentConfigUseCase manageAgentConfigUseCase;
    private final DocumentChunker documentChunker;

    public AgentCliRunner(
            AgentChatUseCase agentChatUseCase,
            ManageAgentConfigUseCase manageAgentConfigUseCase,
            DocumentChunker documentChunker) {
        this.agentChatUseCase = agentChatUseCase;
        this.manageAgentConfigUseCase = manageAgentConfigUseCase;
        this.documentChunker = documentChunker != null ? documentChunker : new DocumentChunker();
    }

    public AgentCliRunner(AgentChatUseCase agentChatUseCase, ManageAgentConfigUseCase manageAgentConfigUseCase) {
        this(agentChatUseCase, manageAgentConfigUseCase, new DocumentChunker());
    }

    public String execute(String... args) {
        if (args == null || args.length == 0) {
            return "Usage: java -jar app.jar agent <chat|config|chunk> [options]";
        }

        int startIndex = 0;
        if ("agent".equalsIgnoreCase(args[0]) && args.length > 1) {
            startIndex = 1;
        }

        String command = args[startIndex].toLowerCase();

        switch (command) {
            case "chat":
                if (args.length <= startIndex + 1) {
                    return "Error: Missing prompt message for chat command.";
                }
                StringBuilder promptBuilder = new StringBuilder();
                for (int i = startIndex + 1; i < args.length; i++) {
                    if (i > startIndex + 1) promptBuilder.append(" ");
                    promptBuilder.append(args[i]);
                }
                if (agentChatUseCase != null) {
                    return agentChatUseCase.chat(promptBuilder.toString());
                } else {
                    return "AgentChatUseCase not configured.";
                }

            case "config":
                if (manageAgentConfigUseCase == null) {
                    return "ManageAgentConfigUseCase not configured.";
                }
                if (args.length > startIndex + 1 && "set".equalsIgnoreCase(args[startIndex + 1])) {
                    String provider = "ollama";
                    String url = "http://localhost:11434";
                    String model = "llama3";
                    ModelTier tier = ModelTier.SMALL;
                    String key = "";
                    for (int i = startIndex + 2; i < args.length - 1; i += 2) {
                        String flag = args[i];
                        String val = args[i + 1];
                        if ("--provider".equalsIgnoreCase(flag)) provider = val;
                        if ("--url".equalsIgnoreCase(flag)) url = val;
                        if ("--model".equalsIgnoreCase(flag)) model = val;
                        if ("--tier".equalsIgnoreCase(flag)) {
                            try {
                                tier = ModelTier.valueOf(val.toUpperCase());
                            } catch (Exception ignored) {
                            }
                        }
                        if ("--key".equalsIgnoreCase(flag)) key = val;
                    }
                    AgentConfig newConfig = new AgentConfig(provider, url, model, tier, key, 0);
                    manageAgentConfigUseCase.updateAgentConfig(newConfig);
                    return "Updated configuration: " + newConfig;
                } else {
                    return "Current LLM Config: " + manageAgentConfigUseCase.getAgentConfig();
                }

            case "chunk":
            case "extract-skills":
                if (args.length <= startIndex + 1) {
                    return "Error: Missing text content to chunk.";
                }
                ModelTier tier = ModelTier.SMALL;
                StringBuilder textBuilder = new StringBuilder();
                for (int i = startIndex + 1; i < args.length; i++) {
                    if ("--tier".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                        try {
                            tier = ModelTier.valueOf(args[i + 1].toUpperCase());
                        } catch (Exception ignored) {
                        }
                        i++;
                    } else {
                        if (textBuilder.length() > 0) textBuilder.append(" ");
                        textBuilder.append(args[i]);
                    }
                }
                List<String> chunks = documentChunker.chunkText(textBuilder.toString(), tier);
                return "Text chunked into " + chunks.size() + " chunk(s) using tier " + tier + ":\n"
                        + String.join("\n---\n", chunks);

            case "sync-inbox":
                return "Triggered inbox synchronization.";

            default:
                return "Unknown agent command: " + command;
        }
    }

    public int run(String... args) {
        String result = execute(args);
        if (result.startsWith("Unknown agent command:") || result.startsWith("Error:")) {
            System.err.println(result);
            return 1;
        }
        System.out.println(result);
        return 0;
    }
}
