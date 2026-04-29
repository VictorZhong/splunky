package com.wpb.spky.persistence;

import com.wpb.spky.session.UserSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "false")
public class NoopSessionMetadataStore implements SessionMetadataStore {

    @Override
    public void recordStarted(UserSession session) {
    }

    @Override
    public void recordTouched(UserSession session) {
    }

    @Override
    public void recordEnded(UserSession session) {
    }
}
