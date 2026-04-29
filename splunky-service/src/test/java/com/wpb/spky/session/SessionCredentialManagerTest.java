package com.wpb.spky.session;

import com.wpb.spky.config.SplunkyProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCredentialManagerTest {

    @Test
    void acceptsFrontendGeneratedSessionWhenCompatibilityModeIsEnabled() {
        SessionCredentialManager manager = new SessionCredentialManager(properties(true));
        UUID sessionId = UUID.randomUUID();

        UserSession session = manager.require(sessionId.toString());

        assertThat(session.sessionId()).isEqualTo(sessionId);
        assertThat(session.splunkPassword()).isNull();
        assertThat(session.username()).isEqualTo("frontend-session");
    }

    @Test
    void storesSplunkPasswordOnlyInRuntimeSession() {
        SessionCredentialManager manager = new SessionCredentialManager(properties(false));

        var response = manager.start("zhong.zc", "secret-password", "SIT");
        UserSession session = manager.require(response.sessionId().toString());

        assertThat(session.splunkPassword()).isEqualTo("secret-password");
        assertThat(response.splunkUsername()).isEqualTo("zhong.zc");
    }

    private static SplunkyProperties properties(boolean acceptFrontendSessions) {
        return new SplunkyProperties(
                new SplunkyProperties.SessionProperties(480, acceptFrontendSessions),
                null
        );
    }
}
