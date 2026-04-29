package com.wpb.spky.llm;

import java.util.List;

public record LlmCompletionResponse(
        LlmProviderType provider,
        String model,
        String content,
        List<ToolCall> toolCalls
) {
    public LlmCompletionResponse(LlmProviderType provider, String model, String content) {
        this(provider, model, content, List.of());
    }

    public record ToolCall(
            String id,
            String name,
            String arguments
    ) {}
}
