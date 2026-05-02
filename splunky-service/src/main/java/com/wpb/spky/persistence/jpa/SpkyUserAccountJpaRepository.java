package com.wpb.spky.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpkyUserAccountJpaRepository extends JpaRepository<SpkyUserAccountEntity, UUID> {

    Optional<SpkyUserAccountEntity> findByUsername(String username);
}
