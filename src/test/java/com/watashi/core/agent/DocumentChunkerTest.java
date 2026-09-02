package com.watashi.core.agent;

import com.watashi.core.agent.AgentConfig.ModelTier;
import java.util.List;
import junit.framework.TestCase;

public class DocumentChunkerTest extends TestCase {

    private DocumentChunker chunker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        chunker = new DocumentChunker();
    }

    public void testChunkTextSmallTier() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 250; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString().trim();

        List<String> chunks = chunker.chunkText(text, ModelTier.SMALL);
        assertEquals(3, chunks.size());
        assertEquals(100, chunks.get(0).split("\\s+").length);
        assertEquals(100, chunks.get(1).split("\\s+").length);
        assertEquals(50, chunks.get(2).split("\\s+").length);
    }

    public void testChunkTextMediumTier() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 600; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString().trim();

        List<String> chunks = chunker.chunkText(text, ModelTier.MEDIUM);
        assertEquals(2, chunks.size());
        assertEquals(500, chunks.get(0).split("\\s+").length);
        assertEquals(100, chunks.get(1).split("\\s+").length);
    }

    public void testChunkTextLargeTier() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 600; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString().trim();

        List<String> chunks = chunker.chunkText(text, ModelTier.LARGE);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    public void testChunkTextNullOrEmpty() {
        assertTrue(chunker.chunkText(null, ModelTier.SMALL).isEmpty());
        assertTrue(chunker.chunkText("", ModelTier.SMALL).isEmpty());
        assertTrue(chunker.chunkText("   ", ModelTier.SMALL).isEmpty());
    }
}
