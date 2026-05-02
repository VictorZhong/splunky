package com.wpb.spky.llm;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;

import java.time.Instant;
import java.util.Optional;

public interface LlmCredentialStore {

    Optional<Credential> find(LlmProviderType provider);

    LlmCredentialStatus status(LlmProviderType provider);

    void upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt);

    void bootstrapIfMissing(LlmProviderType provider, String apiKey, String sessionToken);

    record Credential(
            LlmProviderType provider,
            String apiKey,
            String sessionToken,
            Instant sessionTokenExpiresAt,
            Instant updatedAt
    ) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasFreshSessionToken(Instant now) {
            return sessionToken != null && !sessionToken.isBlank()
                    && sessionTokenExpiresAt != null
                    && sessionTokenExpiresAt.isAfter(now.plusSeconds(30));
        }

        public boolean hasSessionToken() {
            return sessionToken != null && !sessionToken.isBlank();
        }
    }
}
