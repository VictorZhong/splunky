package com.wpb.spky.llm;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class LlmCredentialDtos {

    private LlmCredentialDtos() {}

    public record LlmCredentialStatus(
            boolean configured,
            String provider,
            String status,
            Instant expiresAt,
            Instant lastRefreshedAt,
            String secretFingerprint
    ) {}

    public record UpsertLlmCredentialRequest(
            @NotBlank String provider,
            @NotBlank String credentialType,
            @NotBlank String secret,
            Instant expiresAt
    ) {}
}
