package com.wpb.spky.persistence;

import com.wpb.spky.session.UserSession;

import java.util.Map;
import java.util.UUID;

public interface AuditEventStore {

    void record(UserSession session, String eventType, String targetType, UUID targetId,
                String summary, Map<String, Object> metadata);
}
