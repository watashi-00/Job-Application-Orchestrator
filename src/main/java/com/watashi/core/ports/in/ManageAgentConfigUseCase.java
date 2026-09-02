package com.watashi.core.ports.in;

import com.watashi.core.agent.AgentConfig;

public interface ManageAgentConfigUseCase {

    AgentConfig getAgentConfig();

    void updateAgentConfig(AgentConfig config);

    default AgentConfig getConfig() {
        return getAgentConfig();
    }

    default void updateConfig(AgentConfig config) {
        updateAgentConfig(config);
    }
}
