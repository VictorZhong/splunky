package com.wpb.spky.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "splunky")
public record SplunkyProperties(
        SessionProperties session,
        LlmProperties llm
) {
    public int sessionTtlMinutes() {
        return session == null || session.ttlMinutes() == null ? 480 : session.ttlMinutes();
    }

    public boolean acceptFrontendGeneratedSessions() {
        return session != null
                && session.acceptFrontendGeneratedSessions() != null
                && session.acceptFrontendGeneratedSessions();
    }

    public LlmProperties llmOrDefaults() {
        return llm == null ? new LlmProperties(null, null, null, null) : llm;
    }

    public record SessionProperties(
            Integer ttlMinutes,
            Boolean acceptFrontendGeneratedSessions
    ) {}

    public record LlmProperties(
            String primaryProvider,
            String fallbackProvider,
            CopilotProperties copilot,
            RemoteProperties remote
    ) {
        public CopilotProperties copilotOrDefaults() {
            return copilot == null ? new CopilotProperties(null, null, null, null, null, null, null, null) : copilot;
        }

        public RemoteProperties remoteOrDefaults() {
            return remote == null ? new RemoteProperties(null, null, null, null) : remote;
        }
    }

    public record CopilotProperties(
            String bootstrapApiKey,
            String bootstrapSessionToken,
            String model,
            Integer maxCompletionTokens,
            String editorVersion,
            String baseUrl,
            String tokenUrl,
            String proxyUrl
    ) {
        public String modelOrDefault() {
            return isBlank(model) ? "gpt-5.4" : model;
        }

        public int maxCompletionTokensOrDefault() {
            return maxCompletionTokens == null ? 2048 : maxCompletionTokens;
        }

        public String editorVersionOrDefault() {
            return isBlank(editorVersion) ? "1.114.0" : editorVersion;
        }
    }

    public record RemoteProperties(
            String baseUrl,
            String apiKey,
            String model,
            Integer maxCompletionTokens
    ) {
        public String modelOrDefault() {
            return isBlank(model) ? "gpt-5.4" : model;
        }

        public int maxCompletionTokensOrDefault() {
            return maxCompletionTokens == null ? 2048 : maxCompletionTokens;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
