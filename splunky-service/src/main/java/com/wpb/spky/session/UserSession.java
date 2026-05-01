package com.wpb.spky.session;

import java.time.Instant;
import java.util.UUID;

public record UserSession(
        UUID sessionId,
        UUID userId,
        String username,
        String splunkPassword,
        String environment,
        SessionStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant lastActivityAt
) {
    public boolean isActive(Instant now) {
        return status == SessionStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public UserSession touch(Instant now, Instant expiresAt) {
        return new UserSession(sessionId, userId, username, splunkPassword, environment,
                status, startedAt, expiresAt, now);
    }

    public UserSession logout(Instant now) {
        return new UserSession(sessionId, userId, username, null, environment,
                SessionStatus.LOGGED_OUT, startedAt, now, now);
    }

    public UserSession expire(Instant now) {
        return new UserSession(sessionId, userId, username, null, environment,
                SessionStatus.EXPIRED, startedAt, now, now);
    }

    public enum SessionStatus {
        ACTIVE,
        EXPIRED,
        LOGGED_OUT
    }
}
