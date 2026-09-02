package com.watashi.core.ports.out;

import com.watashi.core.agent.AgentConfig;

public interface AgentConfigRepository {
    AgentConfig loadConfig();

    void saveConfig(AgentConfig config);

    default AgentConfig load() {
        return loadConfig();
    }

    default void save(AgentConfig config) {
        saveConfig(config);
    }
}
