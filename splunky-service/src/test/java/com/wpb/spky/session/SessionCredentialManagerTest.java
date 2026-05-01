package com.wpb.spky.session;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.persistence.NoopSessionMetadataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCredentialManagerTest {

    @Test
    void acceptsFrontendGeneratedSessionWhenCompatibilityModeIsEnabled() {
        SessionCredentialManager manager = new SessionCredentialManager(properties(true), new NoopSessionMetadataStore());
        UUID sessionId = UUID.randomUUID();

        UserSession session = manager.require(sessionId.toString());

        assertThat(session.sessionId()).isEqualTo(sessionId);
        assertThat(session.splunkPassword()).isNull();
        assertThat(session.username()).isEqualTo("frontend-session");
    }

    @Test
    void storesSplunkPasswordOnlyInRuntimeSession() {
        SessionCredentialManager manager = new SessionCredentialManager(properties(false), new NoopSessionMetadataStore());

        var response = manager.start("zhong.zc", "secret-password", "SIT");
        UserSession session = manager.require(response.sessionId().toString());

        assertThat(session.splunkPassword()).isEqualTo("secret-password");
        assertThat(response.splunkUsername()).isEqualTo("zhong.zc");
    }

    @Test
    void sessionExpirySlidesOnActivityAndExpiresAfterIdleTimeout() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-01T00:00:00Z"));
        SessionCredentialManager manager = new SessionCredentialManager(
                properties(false), clock, new NoopSessionMetadataStore());

        var response = manager.start("zhong.zc", "secret-password", "SIT");
        clock.advance(Duration.ofMinutes(20));
        UserSession touched = manager.require(response.sessionId().toString());

        assertThat(touched.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(30)));

        clock.advance(Duration.ofMinutes(31));
        assertThat(manager.find(response.sessionId().toString())).isEmpty();
    }

    @Test
    void logoutImmediatelyExpiresSession() {
        SessionCredentialManager manager = new SessionCredentialManager(properties(false), new NoopSessionMetadataStore());

        var response = manager.start("zhong.zc", "secret-password", "SIT");
        manager.logout(response.sessionId().toString());

        assertThat(manager.find(response.sessionId().toString())).isEmpty();
    }

    private static SplunkyProperties properties(boolean acceptFrontendSessions) {
        return new SplunkyProperties(
                new SplunkyProperties.PersistenceProperties(false),
                new SplunkyProperties.CryptoProperties(null),
                new SplunkyProperties.SessionProperties(30, acceptFrontendSessions),
                null
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
