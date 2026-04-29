package com.wpb.spky.llm;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;

import java.time.Instant;
import java.util.Optional;

public interface LlmCredentialStore {

    Optional<Credential> find(LlmProviderType provider);

    LlmCredentialStatus status(LlmProviderType provider);

    LlmCredentialStatus upsertApiKey(LlmProviderType provider, String apiKey);

    LlmCredentialStatus upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt);

    record Credential(
            LlmProviderType provider,
            String apiKey,
            String sessionToken,
            Instant sessionTokenExpiresAt,
            Instant lastRefreshedAt,
            String secretFingerprint
    ) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }

        public boolean hasFreshSessionToken(Instant now) {
            return sessionToken != null && !sessionToken.isBlank()
                    && sessionTokenExpiresAt != null
                    && sessionTokenExpiresAt.isAfter(now.plusSeconds(30));
        }
    }
}
