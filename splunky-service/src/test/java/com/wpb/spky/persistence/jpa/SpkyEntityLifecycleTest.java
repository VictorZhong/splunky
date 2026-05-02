package com.wpb.spky.persistence.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpkyEntityLifecycleTest {

    @Test
    void userSessionPrePersistFillsTimestamps() {
        SpkyUserSessionEntity entity = new SpkyUserSessionEntity();

        entity.prePersist();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getStartedAt()).isNotNull();
    }

    @Test
    void userAccountLifecycleFillsAndUpdatesTimestamps() {
        SpkyUserAccountEntity entity = new SpkyUserAccountEntity();

        entity.prePersist();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");

        entity.setUpdatedAt(null);
        entity.preUpdate();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void llmCredentialLifecycleFillsAndUpdatesTimestamps() {
        SpkyLlmCredentialEntity entity = new SpkyLlmCredentialEntity();

        entity.prePersist();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");

        entity.setUpdatedAt(null);
        entity.preUpdate();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    void auditEventPrePersistFillsDefaults() {
        SpkyAuditEventEntity entity = new SpkyAuditEventEntity();

        entity.prePersist();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getMetadataJson()).isEqualTo("{}");
    }
}
