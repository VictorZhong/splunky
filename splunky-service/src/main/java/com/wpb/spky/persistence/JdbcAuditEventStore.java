package com.wpb.spky.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuditEventStore implements AuditEventStore {

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    public JdbcAuditEventStore(JdbcClient jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void record(UserSession session, String eventType, String targetType, UUID targetId,
                       String summary, Map<String, Object> metadata) {
        jdbc.sql("""
                insert into spky_audit_event (
                    audit_id, user_id, session_id, staff_id, event_type,
                    target_type, target_id, summary, metadata_json, created_at
                ) values (
                    :auditId, :userId, :sessionId, :staffId, :eventType,
                    :targetType, :targetId, :summary, cast(:metadataJson as jsonb), now()
                )
                """)
                .param("auditId", UUID.randomUUID())
                .param("userId", session == null ? null : session.userId())
                .param("sessionId", session == null ? null : session.sessionId())
                .param("staffId", session == null ? null : session.username())
                .param("eventType", eventType)
                .param("targetType", targetType)
                .param("targetId", targetId)
                .param("summary", summary)
                .param("metadataJson", metadataJson(metadata))
                .update();
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize audit metadata.", ex);
        }
    }
}
