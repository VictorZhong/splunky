package com.wpb.spky.persistence;

import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSessionMetadataStore implements SessionMetadataStore {

    private final JdbcClient jdbc;

    public JdbcSessionMetadataStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void recordStarted(UserSession session) {
        UUID dbUserId = ensureUser(session);
        jdbc.sql("""
                insert into spky_user_session (
                    session_id, user_id, splunk_username, environment, status,
                    started_at, expires_at, last_activity_at, created_at
                ) values (
                    :sessionId, :userId, :username, :environment, :status,
                    :startedAt, :expiresAt, :lastActivityAt, now()
                )
                on conflict (session_id) do update set
                    status = excluded.status,
                    expires_at = excluded.expires_at,
                    last_activity_at = excluded.last_activity_at
                """)
                .param("sessionId", session.sessionId())
                .param("userId", dbUserId)
                .param("username", session.username())
                .param("environment", session.environment())
                .param("status", session.status().name())
                .param("startedAt", ts(session.startedAt()))
                .param("expiresAt", ts(session.expiresAt()))
                .param("lastActivityAt", ts(session.lastActivityAt()))
                .update();
    }

    @Override
    public void recordTouched(UserSession session) {
        jdbc.sql("""
                update spky_user_session
                set last_activity_at = :lastActivityAt
                where session_id = :sessionId
                """)
                .param("lastActivityAt", ts(session.lastActivityAt()))
                .param("sessionId", session.sessionId())
                .update();
    }

    @Override
    public void recordEnded(UserSession session) {
        jdbc.sql("""
                update spky_user_session
                set status = :status,
                    ended_at = :endedAt,
                    expires_at = :endedAt,
                    last_activity_at = :endedAt
                where session_id = :sessionId
                """)
                .param("status", session.status().name())
                .param("endedAt", ts(session.lastActivityAt()))
                .param("sessionId", session.sessionId())
                .update();
    }

    private UUID ensureUser(UserSession session) {
        return jdbc.sql("""
                insert into spky_user_account (
                    user_id, username, display_name, status, last_login_at, created_at, updated_at
                ) values (
                    :userId, :username, :displayName, 'ACTIVE', :lastLoginAt, now(), now()
                )
                on conflict (username) do update set
                    last_login_at = excluded.last_login_at,
                    updated_at = now()
                returning user_id
                """)
                .param("userId", session.userId())
                .param("username", session.username())
                .param("displayName", session.username())
                .param("lastLoginAt", ts(session.startedAt()))
                .query((rs, rowNum) -> rs.getObject("user_id", UUID.class))
                .single();
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
