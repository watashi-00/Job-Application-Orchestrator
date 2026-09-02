package com.watashi.adapters.out.persistence.json;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.ports.out.AgentConfigRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

public class JsonAgentConfigRepositoryTest extends TestCase {

    public void testDefaultConfigWhenFileDoesNotExist() throws Exception {
        Path tempFile = Files.createTempFile("agent-config-test-default", ".json");
        Files.deleteIfExists(tempFile);
        try {
            AgentConfigRepository repo = new JsonAgentConfigRepository(tempFile);
            AgentConfig config = repo.loadConfig();
            assertNotNull(config);
            assertEquals("ollama", config.provider());
            assertEquals("http://localhost:11434", config.ollamaUrl());
            assertEquals("llama3", config.modelName());
            assertEquals(ModelTier.SMALL, config.modelTier());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public void testSaveAndLoadConfig() throws Exception {
        Path tempFile = Files.createTempFile("agent-config-test", ".json");
        try {
            AgentConfigRepository repo = new JsonAgentConfigRepository(tempFile);
            AgentConfig customConfig = new AgentConfig(
                    "ollama", "http://localhost:11434", "llama3:70b", ModelTier.MEDIUM, "secret-key", 300);

            repo.saveConfig(customConfig);

            AgentConfigRepository repo2 = new JsonAgentConfigRepository(tempFile);
            AgentConfig loaded = repo2.loadConfig();

            assertEquals("ollama", loaded.provider());
            assertEquals("http://localhost:11434", loaded.ollamaUrl());
            assertEquals("llama3:70b", loaded.modelName());
            assertEquals(ModelTier.MEDIUM, loaded.modelTier());
            assertEquals("secret-key", loaded.apiKey());
            assertEquals(300, loaded.customChunkSize());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
