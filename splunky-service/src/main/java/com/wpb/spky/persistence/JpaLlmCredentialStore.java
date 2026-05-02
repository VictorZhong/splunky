package com.wpb.spky.persistence;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import com.wpb.spky.llm.LlmCredentialStore;
import com.wpb.spky.llm.LlmProviderType;
import com.wpb.spky.persistence.jpa.SpkyLlmCredentialEntity;
import com.wpb.spky.persistence.jpa.SpkyLlmCredentialJpaRepository;
import com.wpb.spky.persistence.jpa.SpkyUserAccountEntity;
import com.wpb.spky.persistence.jpa.SpkyUserAccountJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JpaLlmCredentialStore implements LlmCredentialStore {

    private static final String API_KEY = "API_KEY";
    private static final String SESSION_TOKEN = "SESSION_TOKEN";
    private static final UUID USER_ID = SystemUserIds.SYSTEM_USER_ID;

    private final SpkyLlmCredentialJpaRepository credentials;
    private final SpkyUserAccountJpaRepository userAccounts;

    public JpaLlmCredentialStore(SpkyLlmCredentialJpaRepository credentials,
                                 SpkyUserAccountJpaRepository userAccounts) {
        this.credentials = credentials;
        this.userAccounts = userAccounts;
    }

    @Override
    public Optional<Credential> find(LlmProviderType provider) {
        ensureSystemUser();
        List<SpkyLlmCredentialEntity> rows = credentials.findByUserIdAndProviderAndCredentialTypeInAndStatus(
                USER_ID,
                provider.name(),
                List.of(API_KEY, SESSION_TOKEN),
                "ACTIVE"
        );

        String apiKey = null;
        String sessionToken = null;
        Instant sessionExpiresAt = null;
        Instant lastRefreshedAt = null;
        String fingerprint = null;
        for (SpkyLlmCredentialEntity row : rows) {
            if (API_KEY.equals(row.getCredentialType())) {
                apiKey = row.getEncryptedSecret();
                fingerprint = firstNonBlank(fingerprint, row.getSecretFingerprint());
            }
            if (SESSION_TOKEN.equals(row.getCredentialType())) {
                sessionToken = row.getEncryptedSecret();
                sessionExpiresAt = row.getExpiresAt();
                lastRefreshedAt = row.getLastRefreshedAt();
                fingerprint = firstNonBlank(fingerprint, row.getSecretFingerprint());
            }
        }

        if (isBlank(apiKey) && isBlank(sessionToken)) return Optional.empty();
        return Optional.of(new Credential(provider, apiKey, sessionToken, sessionExpiresAt, lastRefreshedAt, fingerprint));
    }

    @Override
    public LlmCredentialStatus status(LlmProviderType provider) {
        return find(provider)
                .map(credential -> {
                    Instant now = Instant.now();
                    String status = credential.hasApiKey() || credential.hasFreshSessionToken(now) ? "ACTIVE" : "EXPIRED";
                    return new LlmCredentialStatus(true, provider.name(), status,
                            credential.sessionTokenExpiresAt(), credential.lastRefreshedAt(),
                            credential.secretFingerprint());
                })
                .orElseGet(() -> new LlmCredentialStatus(false, provider.name(), "NOT_CONFIGURED", null, null, null));
    }

    @Override
    @Transactional
    public LlmCredentialStatus upsertApiKey(LlmProviderType provider, String apiKey) {
        if (isBlank(apiKey)) throw new IllegalArgumentException("LLM API key must not be blank.");
        ensureSystemUser();
        upsert(provider, API_KEY, apiKey, null, null);
        deleteCredential(provider, SESSION_TOKEN);
        return status(provider);
    }

    @Override
    @Transactional
    public LlmCredentialStatus upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt) {
        ensureSystemUser();
        if (isBlank(sessionToken)) {
            deleteCredential(provider, SESSION_TOKEN);
        } else {
            upsert(provider, SESSION_TOKEN, sessionToken, expiresAt, Instant.now());
        }
        return status(provider);
    }

    private void upsert(LlmProviderType provider, String credentialType, String secret,
                        Instant expiresAt, Instant lastRefreshedAt) {
        SpkyLlmCredentialEntity entity = credentials
                .findByUserIdAndProviderAndCredentialType(USER_ID, provider.name(), credentialType)
                .orElseGet(SpkyLlmCredentialEntity::new);

        if (entity.getCredentialId() == null) {
            entity.setCredentialId(UUID.randomUUID());
            entity.setUserId(USER_ID);
            entity.setProvider(provider.name());
            entity.setCredentialType(credentialType);
            entity.setCreatedAt(Instant.now());
        }

        entity.setEncryptedSecret(secret);
        entity.setSecretFingerprint(SecretCipher.fingerprint(secret));
        entity.setExpiresAt(expiresAt);
        entity.setLastRefreshedAt(lastRefreshedAt);
        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(Instant.now());
        credentials.save(entity);
    }

    private void deleteCredential(LlmProviderType provider, String credentialType) {
        credentials.deleteByUserIdAndProviderAndCredentialType(USER_ID, provider.name(), credentialType);
    }

    private void ensureSystemUser() {
        if (userAccounts.findByUsername(SystemUserIds.SYSTEM_USERNAME).isPresent()) {
            return;
        }

        SpkyUserAccountEntity entity = new SpkyUserAccountEntity();
        Instant now = Instant.now();
        entity.setUserId(USER_ID);
        entity.setUsername(SystemUserIds.SYSTEM_USERNAME);
        entity.setDisplayName("System");
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userAccounts.save(entity);
    }

    private static String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
