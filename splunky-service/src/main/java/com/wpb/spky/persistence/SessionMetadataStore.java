package com.wpb.spky.persistence;

import com.wpb.spky.session.UserSession;

public interface SessionMetadataStore {

    void recordStarted(UserSession session);

    void recordTouched(UserSession session);

    void recordEnded(UserSession session);
}
