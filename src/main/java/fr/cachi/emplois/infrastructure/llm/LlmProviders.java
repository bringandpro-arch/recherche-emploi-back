package fr.cachi.emplois.infrastructure.llm;

import fr.cachi.emplois.domain.port.LlmProvider;

/** Fabrique du provider LLM selon la configuration ({@code LLM_PROVIDER}). */
public final class LlmProviders {

    private LlmProviders() {
    }

    public static LlmProvider fromEnv() {
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "bedrock");
        if ("noop".equalsIgnoreCase(provider)) {
            return new NoopLlmProvider();
        }
        return new BedrockLlmProvider();
    }
}
