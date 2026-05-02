package com.wpb.spky.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.persistence.jpa.SpkyAuditEventEntity;
import com.wpb.spky.persistence.jpa.SpkyAuditEventJpaRepository;
import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JpaAuditEventStore implements AuditEventStore {

    private final SpkyAuditEventJpaRepository auditEvents;
    private final ObjectMapper mapper;

    public JpaAuditEventStore(SpkyAuditEventJpaRepository auditEvents, ObjectMapper mapper) {
        this.auditEvents = auditEvents;
        this.mapper = mapper;
    }

    @Override
    public void record(UserSession session, String eventType, String targetType, UUID targetId,
                       String summary, Map<String, Object> metadata) {
        SpkyAuditEventEntity entity = new SpkyAuditEventEntity();
        entity.setAuditId(UUID.randomUUID());
        entity.setUserId(session == null ? null : session.userId());
        entity.setSessionId(session == null ? null : session.sessionId());
        entity.setStaffId(session == null ? null : session.username());
        entity.setEventType(eventType);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setSummary(summary);
        entity.setMetadataJson(metadataJson(metadata));
        entity.setCreatedAt(Instant.now());
        auditEvents.save(entity);
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize audit metadata.", ex);
        }
    }
}
