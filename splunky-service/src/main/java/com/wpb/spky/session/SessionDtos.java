package com.wpb.spky.session;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class SessionDtos {

    private SessionDtos() {}

    public record SplunkLoginRequest(
            @NotBlank String splunkUsername,
            @NotBlank String splunkPassword,
            @NotBlank String environment
    ) {}

    public record SessionResponse(
            UUID sessionId,
            UserProfile user,
            String splunkUsername,
            String environment,
            String status,
            Instant startedAt,
            Instant expiresAt,
            Instant lastActivityAt
    ) {}

    public record UserProfile(
            UUID userId,
            String username,
            String displayName,
            String email,
            String status
    ) {}
}
