package com.wpb.spky.persistence;

import com.wpb.spky.persistence.jpa.SpkyUserAccountEntity;
import com.wpb.spky.persistence.jpa.SpkyUserAccountJpaRepository;
import com.wpb.spky.persistence.jpa.SpkyUserSessionEntity;
import com.wpb.spky.persistence.jpa.SpkyUserSessionJpaRepository;
import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JpaSessionMetadataStore implements SessionMetadataStore {

    private final SpkyUserAccountJpaRepository userAccounts;
    private final SpkyUserSessionJpaRepository userSessions;

    public JpaSessionMetadataStore(SpkyUserAccountJpaRepository userAccounts,
                                   SpkyUserSessionJpaRepository userSessions) {
        this.userAccounts = userAccounts;
        this.userSessions = userSessions;
    }

    @Override
    @Transactional
    public void recordStarted(UserSession session) {
        UUID userId = ensureUser(session);
        SpkyUserSessionEntity entity = userSessions.findById(session.sessionId()).orElseGet(SpkyUserSessionEntity::new);
        entity.setSessionId(session.sessionId());
        entity.setUserId(userId);
        entity.setSplunkUsername(session.username());
        entity.setEnvironment(session.environment());
        entity.setStatus(session.status().name());
        entity.setStartedAt(session.startedAt());
        entity.setExpiresAt(session.expiresAt());
        entity.setLastActivityAt(session.lastActivityAt());
        userSessions.save(entity);
    }

    @Override
    @Transactional
    public void recordTouched(UserSession session) {
        userSessions.findById(session.sessionId()).ifPresent(entity -> {
            entity.setLastActivityAt(session.lastActivityAt());
            entity.setExpiresAt(session.expiresAt());
            userSessions.save(entity);
        });
    }

    @Override
    @Transactional
    public void recordEnded(UserSession session) {
        userSessions.findById(session.sessionId()).ifPresent(entity -> {
            Instant endedAt = session.lastActivityAt();
            entity.setStatus(session.status().name());
            entity.setEndedAt(endedAt);
            entity.setExpiresAt(endedAt);
            entity.setLastActivityAt(endedAt);
            userSessions.save(entity);
        });
    }

    private UUID ensureUser(UserSession session) {
        Instant now = Instant.now();
        SpkyUserAccountEntity entity = userAccounts.findByUsername(session.username())
                .orElseGet(SpkyUserAccountEntity::new);

        if (entity.getUserId() == null) {
            entity.setUserId(session.userId());
            entity.setCreatedAt(now);
            entity.setCreatedByStaffId(session.username());
        }

        entity.setUsername(session.username());
        entity.setDisplayName(session.username());
        entity.setStatus("ACTIVE");
        entity.setLastLoginAt(session.startedAt());
        entity.setUpdatedByStaffId(session.username());
        entity.setUpdatedAt(now);

        return userAccounts.save(entity).getUserId();
    }
}
