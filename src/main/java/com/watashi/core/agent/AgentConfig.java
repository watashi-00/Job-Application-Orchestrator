package com.watashi.core.agent;

public record AgentConfig(
        String provider, String ollamaUrl, String modelName, ModelTier modelTier, String apiKey, int customChunkSize) {

    public AgentConfig {
        if (provider == null) {
            provider = "ollama";
        }
        if (ollamaUrl == null) {
            ollamaUrl = "http://localhost:11434";
        }
        if (modelName == null) {
            modelName = "llama3";
        }
        if (modelTier == null) {
            modelTier = ModelTier.SMALL;
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }

    public enum ModelTier {
        SMALL(100),
        MEDIUM(500),
        LARGE(0);

        private final int chunkSize;

        ModelTier(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkSize() {
            return chunkSize;
        }
    }

    public AgentConfig() {
        this("ollama", "http://localhost:11434", "llama3", ModelTier.SMALL, "", 0);
    }

    public static AgentConfig defaultConfig() {
        return new AgentConfig();
    }
}
