package com.wpb.spky.persistence;

import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "false")
public class NoopAuditEventStore implements AuditEventStore {

    @Override
    public void record(UserSession session, String eventType, String targetType, UUID targetId,
                       String summary, Map<String, Object> metadata) {
    }
}
