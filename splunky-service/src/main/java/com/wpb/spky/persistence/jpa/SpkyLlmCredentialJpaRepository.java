package com.wpb.spky.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpkyLlmCredentialJpaRepository extends JpaRepository<SpkyLlmCredentialEntity, UUID> {

    List<SpkyLlmCredentialEntity> findByUserIdAndProviderAndCredentialTypeInAndStatus(
            UUID userId, String provider, Collection<String> credentialTypes, String status);

    Optional<SpkyLlmCredentialEntity> findByUserIdAndProviderAndCredentialType(
            UUID userId, String provider, String credentialType);

    void deleteByUserIdAndProviderAndCredentialType(UUID userId, String provider, String credentialType);
}
