package com.wpb.spky.session;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.session.SessionDtos.SessionResponse;
import com.wpb.spky.session.SessionDtos.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionCredentialManager {

    private final ConcurrentMap<UUID, UserSession> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final boolean acceptFrontendGeneratedSessions;

    @Autowired
    public SessionCredentialManager(SplunkyProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SessionCredentialManager(SplunkyProperties properties, Clock clock) {
        this.clock = clock;
        this.ttl = Duration.ofMinutes(properties.sessionTtlMinutes());
        this.acceptFrontendGeneratedSessions = properties.acceptFrontendGeneratedSessions();
    }

    public SessionResponse start(String username, String password, String environment) {
        if (isBlank(username) || isBlank(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Splunk username and password are required.");
        }
        Instant now = clock.instant();
        UUID sessionId = UUID.randomUUID();
        UUID userId = stableUserId(username);
        UserSession session = new UserSession(sessionId, userId, username.trim(), password,
                normalizeEnvironment(environment), UserSession.SessionStatus.ACTIVE, now, now.plus(ttl), now);
        sessions.put(sessionId, session);
        return toResponse(session);
    }

    public Optional<SessionResponse> find(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) return Optional.empty();
        UserSession session = sessions.get(sessionId);
        if (session == null) return Optional.empty();
        if (!session.isActive(clock.instant())) return Optional.empty();
        UserSession touched = session.touch(clock.instant());
        sessions.put(sessionId, touched);
        return Optional.of(toResponse(touched));
    }

    public UserSession require(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Splunky session.");
        }

        Instant now = clock.instant();
        UserSession existing = sessions.get(sessionId);
        if (existing == null && acceptFrontendGeneratedSessions) {
            existing = createFrontendCompatibilitySession(sessionId, now);
            sessions.put(sessionId, existing);
        }
        if (existing == null || !existing.isActive(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Splunk credentials may be invalid or the session expired. Please log in again.");
        }

        UserSession touched = existing.touch(now);
        sessions.put(sessionId, touched);
        return touched;
    }

    public void logout(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Splunky session.");
        }
        UserSession session = sessions.get(sessionId);
        if (session == null) return;
        sessions.put(sessionId, session.logout(clock.instant()));
    }

    public static String resolveSessionId(String splunkyHeader, String spkyHeader) {
        return !isBlank(splunkyHeader) ? splunkyHeader : spkyHeader;
    }

    public SessionResponse toResponse(UserSession session) {
        return new SessionResponse(
                session.sessionId(),
                new UserProfile(session.userId(), session.username(), session.username(), null, "ACTIVE"),
                session.username(),
                session.environment(),
                session.status().name(),
                session.startedAt(),
                session.expiresAt(),
                session.lastActivityAt()
        );
    }

    private UserSession createFrontendCompatibilitySession(UUID sessionId, Instant now) {
        String username = "frontend-session";
        return new UserSession(sessionId, stableUserId(username), username, null, "SIT",
                UserSession.SessionStatus.ACTIVE, now, now.plus(ttl), now);
    }

    private static UUID parseSessionId(String raw) {
        if (isBlank(raw)) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static UUID stableUserId(String username) {
        return UUID.nameUUIDFromBytes(("splunky-user:" + username.trim().toLowerCase()).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeEnvironment(String environment) {
        return isBlank(environment) ? "SIT" : environment.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
