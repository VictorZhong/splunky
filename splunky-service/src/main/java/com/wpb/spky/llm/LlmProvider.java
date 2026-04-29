package com.wpb.spky.llm;

public interface LlmProvider {

    LlmProviderType providerType();

    default boolean isAvailable() {
        return false;
    }

    default LlmCompletionResponse complete(LlmCompletionRequest request) {
        throw new UnsupportedOperationException(providerType() + " provider is not wired");
    }
}
