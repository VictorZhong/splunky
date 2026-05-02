package com.wpb.spky.llm;

import java.time.Instant;

public final class LlmCredentialDtos {

    private LlmCredentialDtos() {}

    public record LlmCredentialStatus(
            boolean configured,
            String provider,
            String status,
            boolean apiKeyConfigured,
            boolean sessionTokenConfigured,
            Instant sessionTokenExpiresAt,
            Instant updatedAt
    ) {}
}
