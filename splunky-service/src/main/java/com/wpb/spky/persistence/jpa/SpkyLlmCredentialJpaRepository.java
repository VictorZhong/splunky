package com.wpb.spky.persistence.jpa;

import com.wpb.spky.llm.LlmProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpkyLlmCredentialJpaRepository extends JpaRepository<SpkyLlmCredentialEntity, LlmProviderType> {}
