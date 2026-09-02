package com.watashi.core.ports.out;

import com.watashi.core.agent.AgentConfig;

public interface LlmProviderPort {
    String generate(String prompt, AgentConfig config);
}
