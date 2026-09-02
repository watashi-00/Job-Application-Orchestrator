package com.watashi.core.ports.in;

import com.watashi.core.agent.AgentConfig;
import java.util.List;

public interface AgentChatUseCase {

    String chat(String message);

    AgentChatResponse chat(AgentChatRequest request);

    public record AgentChatRequest(String message, String context, AgentConfig configOverride) {
        public AgentChatRequest(String message) {
            this(message, null, null);
        }
    }

    public record AgentChatResponse(String response, List<String> toolsExecuted) {
        public AgentChatResponse(String response) {
            this(response, List.of());
        }

        public AgentChatResponse {
            toolsExecuted = toolsExecuted == null ? List.of() : List.copyOf(toolsExecuted);
        }
    }
}
