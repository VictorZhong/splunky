package com.wpb.spky.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpkyQueryTemplateJpaRepository extends JpaRepository<SpkyQueryTemplateEntity, UUID> {

    List<SpkyQueryTemplateEntity> findByEnabledTrueOrderByTemplateKeyAsc();
}
