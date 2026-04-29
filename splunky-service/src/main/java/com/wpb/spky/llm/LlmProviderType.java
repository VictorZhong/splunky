package com.wpb.spky.llm;

import java.util.Locale;

public enum LlmProviderType {
    COPILOT_PERSONAL,
    REMOTE_API;

    public static LlmProviderType parse(String raw, LlmProviderType defaultValue) {
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return LlmProviderType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
