package com.wpb.spky.llm;

import com.wpb.spky.config.SplunkyProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLlmCredentialStoreTest {

    @Test
    void bootstrapsCopilotCredentialFromConfiguration() {
        InMemoryLlmCredentialStore store = new InMemoryLlmCredentialStore(properties("api-key", null));
        store.bootstrap();

        LlmCredentialStore.Credential credential = store.find(LlmProviderType.COPILOT_PERSONAL).orElseThrow();

        assertThat(credential.hasApiKey()).isTrue();
        assertThat(credential.apiKey()).isEqualTo("api-key");
        assertThat(store.status(LlmProviderType.COPILOT_PERSONAL).apiKeyConfigured()).isTrue();
        assertThat(store.status(LlmProviderType.COPILOT_PERSONAL).sessionTokenConfigured()).isFalse();
    }

    @Test
    void sessionTokenWithoutExplicitExpiryGetsDefaultExpiry() {
        InMemoryLlmCredentialStore store = new InMemoryLlmCredentialStore(properties(null, null));

        store.upsertSessionToken(LlmProviderType.COPILOT_PERSONAL, "session-token", null);

        LlmCredentialStore.Credential credential = store.find(LlmProviderType.COPILOT_PERSONAL).orElseThrow();
        assertThat(credential.hasFreshSessionToken(Instant.now())).isTrue();
        assertThat(credential.sessionTokenExpiresAt()).isAfter(Instant.now().plusSeconds(60));
    }

    private static SplunkyProperties properties(String apiKey, String sessionToken) {
        return new SplunkyProperties(
                new SplunkyProperties.PersistenceProperties(false),
                new SplunkyProperties.CryptoProperties(null),
                new SplunkyProperties.SessionProperties(480, true),
                new SplunkyProperties.LlmProperties(
                        "COPILOT_PERSONAL",
                        "REMOTE_API",
                        new SplunkyProperties.CopilotProperties(
                                apiKey,
                                sessionToken,
                                "gpt-5.4",
                                2048,
                                "1.114.0",
                                "https://api.githubcopilot.com",
                                "https://api.github.com/copilot_internal/v2/token",
                                null
                        ),
                        null
                )
        );
    }
}
