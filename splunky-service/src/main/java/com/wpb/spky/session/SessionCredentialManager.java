package com.wpb.spky.session;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.persistence.AuditEventStore;
import com.wpb.spky.persistence.NoopAuditEventStore;
import com.wpb.spky.persistence.NoopSessionMetadataStore;
import com.wpb.spky.persistence.SessionMetadataStore;
import com.wpb.spky.splunk.SplunkSessionService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SessionCredentialManager {

    private final ConcurrentMap<UUID, UserSession> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final boolean acceptFrontendGeneratedSessions;
    private final SessionMetadataStore sessionMetadata;
    private final AuditEventStore auditEvents;
    private final SplunkSessionService splunkSessions;

    @Autowired
    public SessionCredentialManager(SplunkyProperties properties, SessionMetadataStore sessionMetadata,
                                    AuditEventStore auditEvents, SplunkSessionService splunkSessions) {
        this(properties, Clock.systemUTC(), sessionMetadata, auditEvents, splunkSessions);
    }

    SessionCredentialManager(SplunkyProperties properties, Clock clock) {
        this(properties, clock, new NoopSessionMetadataStore(), new NoopAuditEventStore(), null);
    }

    SessionCredentialManager(SplunkyProperties properties, SessionMetadataStore sessionMetadata) {
        this(properties, Clock.systemUTC(), sessionMetadata, new NoopAuditEventStore(), null);
    }

    SessionCredentialManager(SplunkyProperties properties, Clock clock, SessionMetadataStore sessionMetadata) {
        this(properties, clock, sessionMetadata, new NoopAuditEventStore(), null);
    }

    SessionCredentialManager(SplunkyProperties properties, Clock clock, SessionMetadataStore sessionMetadata,
                             AuditEventStore auditEvents, SplunkSessionService splunkSessions) {
        this.clock = clock;
        this.ttl = Duration.ofMinutes(properties.sessionTtlMinutes());
        this.acceptFrontendGeneratedSessions = properties.acceptFrontendGeneratedSessions();
        this.sessionMetadata = sessionMetadata;
        this.auditEvents = auditEvents;
        this.splunkSessions = splunkSessions;
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
        sessionMetadata.recordStarted(session);
        auditEvents.record(session, "SESSION_LOGIN", "SESSION", session.sessionId(),
                "Splunky session started", Map.of("environment", session.environment()));
        return toResponse(session);
    }

    public Optional<SessionResponse> find(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) return Optional.empty();
        UserSession session = touchExistingSession(sessionId, false);
        return session == null || session.status() != UserSession.SessionStatus.ACTIVE
                ? Optional.empty()
                : Optional.of(toResponse(session));
    }

    public UserSession require(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Splunky session.");
        }

        UserSession session = touchExistingSession(sessionId, acceptFrontendGeneratedSessions);
        if (session == null || session.status() != UserSession.SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Splunk credentials may be invalid or the session expired. Please log in again.");
        }
        return session;
    }

    public void logout(String rawSessionId) {
        UUID sessionId = parseSessionId(rawSessionId);
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Splunky session.");
        }
        UserSession session = sessions.get(sessionId);
        if (session == null) return;
        UserSession loggedOut = session.logout(clock.instant());
        sessions.put(sessionId, loggedOut);
        sessionMetadata.recordEnded(loggedOut);
        invalidateSplunkSession(loggedOut.sessionId());
        auditEvents.record(loggedOut, "SESSION_LOGOUT", "SESSION", loggedOut.sessionId(),
                "Splunky session ended", Map.of("environment", loggedOut.environment()));
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

    private UserSession touchExistingSession(UUID sessionId, boolean allowCompatibilitySession) {
        Instant now = clock.instant();
        AtomicReference<UserSession> created = new AtomicReference<>();
        AtomicReference<UserSession> touched = new AtomicReference<>();
        AtomicReference<UserSession> expired = new AtomicReference<>();

        UserSession current = sessions.compute(sessionId, (id, existing) -> {
            UserSession local = existing;
            if (local == null && allowCompatibilitySession) {
                local = createFrontendCompatibilitySession(sessionId, now);
                created.set(local);
            }
            if (local == null) return null;
            if (local.status() == UserSession.SessionStatus.ACTIVE && !local.expiresAt().isAfter(now)) {
                UserSession next = local.expire(now);
                expired.set(next);
                return next;
            }
            if (local.status() != UserSession.SessionStatus.ACTIVE) {
                return local;
            }
            UserSession next = local.touch(now, now.plus(ttl));
            touched.set(next);
            return next;
        });

        UserSession createdSession = created.get();
        if (createdSession != null) {
            sessionMetadata.recordStarted(createdSession);
        }
        UserSession expiredSession = expired.get();
        if (expiredSession != null) {
            sessionMetadata.recordEnded(expiredSession);
            invalidateSplunkSession(expiredSession.sessionId());
            auditEvents.record(expiredSession, "SESSION_EXPIRED", "SESSION", expiredSession.sessionId(),
                    "Splunky session expired after idle timeout", Map.of("environment", expiredSession.environment()));
        }
        UserSession touchedSession = touched.get();
        if (touchedSession != null) {
            sessionMetadata.recordTouched(touchedSession);
        }
        return current;
    }

    private void invalidateSplunkSession(UUID sessionId) {
        if (splunkSessions != null) {
            splunkSessions.invalidate(sessionId);
        }
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
