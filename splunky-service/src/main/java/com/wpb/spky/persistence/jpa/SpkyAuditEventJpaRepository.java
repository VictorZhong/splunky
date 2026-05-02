package com.wpb.spky.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpkyAuditEventJpaRepository extends JpaRepository<SpkyAuditEventEntity, UUID> {
}
