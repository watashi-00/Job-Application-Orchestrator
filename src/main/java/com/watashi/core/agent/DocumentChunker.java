package com.watashi.core.agent;

import com.watashi.core.agent.AgentConfig.ModelTier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DocumentChunker {

    public List<String> chunkText(String rawText, ModelTier tier) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = rawText.trim();
        if (tier == null || tier == ModelTier.LARGE || tier.getChunkSize() <= 0) {
            return List.of(trimmed);
        }

        int chunkSize = tier.getChunkSize();
        String[] words = trimmed.split("\\s+");
        if (words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += chunkSize) {
            int end = Math.min(words.length, i + chunkSize);
            String[] subArray = Arrays.copyOfRange(words, i, end);
            chunks.add(String.join(" ", subArray));
        }

        return chunks;
    }
}
