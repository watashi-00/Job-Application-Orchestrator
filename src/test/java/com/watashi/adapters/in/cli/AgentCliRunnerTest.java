package com.watashi.adapters.in.cli;

import com.watashi.core.agent.AgentConfig;
import com.watashi.core.agent.AgentConfig.ModelTier;
import com.watashi.core.ports.in.AgentChatUseCase;
import com.watashi.core.ports.in.ManageAgentConfigUseCase;
import junit.framework.TestCase;

public class AgentCliRunnerTest extends TestCase {

    private AgentCliRunner cliRunner;
    private AgentConfig currentConfig;
    private String lastChatPrompt;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        currentConfig = new AgentConfig("ollama", "http://localhost:11434", "llama3", ModelTier.SMALL, "", 0);

        AgentChatUseCase chatUseCase = new AgentChatUseCase() {
            @Override
            public String chat(String message) {
                lastChatPrompt = message;
                return "Agent Response to: " + message;
            }

            @Override
            public AgentChatResponse chat(AgentChatRequest request) {
                lastChatPrompt = request.message();
                return new AgentChatResponse("Agent Response to: " + request.message());
            }
        };

        ManageAgentConfigUseCase configUseCase = new ManageAgentConfigUseCase() {
            @Override
            public AgentConfig getAgentConfig() {
                return currentConfig;
            }

            @Override
            public void updateAgentConfig(AgentConfig config) {
                currentConfig = config;
            }
        };

        cliRunner = new AgentCliRunner(chatUseCase, configUseCase);
    }

    public void testExecuteChatCommand() {
        String result = cliRunner.execute("chat", "Summarize my active interview invites");
        assertEquals("Summarize my active interview invites", lastChatPrompt);
        assertTrue(result.contains("Agent Response to: Summarize my active interview invites"));
    }

    public void testExecuteAgentChatCommandWithPrefix() {
        String result = cliRunner.execute("agent", "chat", "Check job vacancies");
        assertEquals("Check job vacancies", lastChatPrompt);
        assertTrue(result.contains("Agent Response to: Check job vacancies"));
    }

    public void testExecuteConfigCommand() {
        String result = cliRunner.execute("config");
        assertTrue(result.contains("llama3"));
    }

    public void testExecuteChunkCommand() {
        String result = cliRunner.execute("chunk", "Java Spring Boot Microservices Docker Kubernetes");
        assertNotNull(result);
        assertTrue(result.contains("chunk") || result.contains("Java") || result.contains("SMALL"));
    }

    public void testRunReturnsExitCode() {
        int code = cliRunner.run("chat", "Hello");
        assertEquals(0, code);

        int errCode = cliRunner.run("unknown-cmd");
        assertFalse(errCode == 0);
    }
}
