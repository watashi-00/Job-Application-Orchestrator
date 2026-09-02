package com.watashi.adapters.out.persistence.json;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.ports.out.AgentConfigRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class JsonAgentConfigRepository implements AgentConfigRepository {

    private final Path filePath;
    private AgentConfig config;

    public JsonAgentConfigRepository() {
        this(DataDirectoryResolver.resolveDataDirectory().resolve("agent-config.json"));
    }

    public JsonAgentConfigRepository(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        if (!Files.exists(filePath)) {
            this.config = AgentConfig.defaultConfig();
            return;
        }
        try {
            if (Files.size(filePath) == 0) {
                this.config = AgentConfig.defaultConfig();
                return;
            }
            AgentConfig loaded = JsonStorageUtils.readJson(filePath, AgentConfig.class);
            this.config = (loaded != null) ? loaded : AgentConfig.defaultConfig();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load agent config from " + filePath, e);
        }
    }

    @Override
    public synchronized AgentConfig loadConfig() {
        return config != null ? config : AgentConfig.defaultConfig();
    }

    @Override
    public synchronized void saveConfig(AgentConfig newConfig) {
        Objects.requireNonNull(newConfig, "config cannot be null");
        this.config = newConfig;
        writeToFile();
    }

    private void writeToFile() {
        try {
            JsonStorageUtils.writeJsonAtomic(filePath, config);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write agent config to " + filePath, e);
        }
    }
}
