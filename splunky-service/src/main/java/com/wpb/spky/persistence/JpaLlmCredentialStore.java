package com.wpb.spky.persistence;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import com.wpb.spky.llm.LlmCredentialStore;
import com.wpb.spky.llm.LlmProviderType;
import com.wpb.spky.persistence.jpa.SpkyLlmCredentialEntity;
import com.wpb.spky.persistence.jpa.SpkyLlmCredentialJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JpaLlmCredentialStore implements LlmCredentialStore {

    private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(25);

    private final SpkyLlmCredentialJpaRepository credentials;
    private final TransactionTemplate transactions;

    @Override
    public Optional<Credential> find(LlmProviderType provider) {
        return credentials.findById(provider).map(this::map);
    }

    @Override
    public LlmCredentialStatus status(LlmProviderType provider) {
        return find(provider)
                .map(credential -> {
                    boolean apiKeyConfigured = credential.hasApiKey();
                    boolean sessionTokenConfigured = credential.hasSessionToken();
                    boolean configured = apiKeyConfigured || sessionTokenConfigured;
                    Instant now = Instant.now();
                    String status = !configured
                            ? "NOT_CONFIGURED"
                            : (apiKeyConfigured || credential.hasFreshSessionToken(now)) ? "ACTIVE" : "EXPIRED";
                    return new LlmCredentialStatus(
                            configured,
                            provider.name(),
                            status,
                            apiKeyConfigured,
                            sessionTokenConfigured,
                            credential.sessionTokenExpiresAt(),
                            credential.updatedAt()
                    );
                })
                .orElseGet(() -> new LlmCredentialStatus(
                        false,
                        provider.name(),
                        "NOT_CONFIGURED",
                        false,
                        false,
                        null,
                        null
                ));
    }

    @Override
    public void upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt) {
        transactions.executeWithoutResult(tx -> {
            SpkyLlmCredentialEntity entity = credentials.findById(provider).orElseGet(() -> {
                SpkyLlmCredentialEntity created = new SpkyLlmCredentialEntity();
                created.setProvider(provider);
                return created;
            });
            entity.setSessionToken(nullIfBlank(sessionToken));
            entity.setSessionTokenExpiresAt(sessionToken == null || sessionToken.isBlank()
                    ? null
                    : resolvedExpiresAt(expiresAt));
            entity.setUpdatedAt(Instant.now());
            credentials.save(entity);
        });
    }

    @Override
    public void bootstrapIfMissing(LlmProviderType provider, String apiKey, String sessionToken) {
        if (isBlank(apiKey) && isBlank(sessionToken)) {
            return;
        }

        transactions.executeWithoutResult(tx -> {
            if (credentials.existsById(provider)) {
                return;
            }
            SpkyLlmCredentialEntity entity = new SpkyLlmCredentialEntity();
            entity.setProvider(provider);
            entity.setApiKey(nullIfBlank(apiKey));
            entity.setSessionToken(nullIfBlank(sessionToken));
            if (!isBlank(sessionToken)) {
                entity.setSessionTokenExpiresAt(Instant.now().plus(DEFAULT_SESSION_TTL));
            }
            entity.setUpdatedAt(Instant.now());
            credentials.save(entity);
        });
    }

    private Credential map(SpkyLlmCredentialEntity entity) {
        return new Credential(
                entity.getProvider(),
                entity.getApiKey(),
                entity.getSessionToken(),
                entity.getSessionTokenExpiresAt(),
                entity.getUpdatedAt()
        );
    }

    private static Instant resolvedExpiresAt(Instant expiresAt) {
        return expiresAt == null ? Instant.now().plus(DEFAULT_SESSION_TTL) : expiresAt;
    }

    private static String nullIfBlank(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
