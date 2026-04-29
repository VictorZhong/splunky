package com.wpb.spky.persistence;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import com.wpb.spky.llm.LlmCredentialStore;
import com.wpb.spky.llm.LlmProviderType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcLlmCredentialStore implements LlmCredentialStore {

    private static final String API_KEY = "API_KEY";
    private static final String SESSION_TOKEN = "SESSION_TOKEN";
    private static final UUID USER_ID = SystemUserIds.SYSTEM_USER_ID;

    private final JdbcClient jdbc;
    private final SecretCipher cipher;

    public JdbcLlmCredentialStore(JdbcClient jdbc, SecretCipher cipher) {
        this.jdbc = jdbc;
        this.cipher = cipher;
    }

    @Override
    public Optional<Credential> find(LlmProviderType provider) {
        ensureSystemUser();
        List<CredentialRow> rows = jdbc.sql("""
                select credential_type, encrypted_secret, secret_fingerprint, expires_at, last_refreshed_at
                from spky_llm_credential
                where user_id = :userId
                  and provider = :provider
                  and credential_type in ('API_KEY', 'SESSION_TOKEN')
                  and status = 'ACTIVE'
                """)
                .param("userId", USER_ID)
                .param("provider", provider.name())
                .query((rs, rowNum) -> new CredentialRow(
                        rs.getString("credential_type"),
                        rs.getString("encrypted_secret"),
                        rs.getString("secret_fingerprint"),
                        rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("last_refreshed_at") == null ? null : rs.getTimestamp("last_refreshed_at").toInstant()
                ))
                .list();

        String apiKey = null;
        String sessionToken = null;
        Instant sessionExpiresAt = null;
        Instant lastRefreshedAt = null;
        String fingerprint = null;
        for (CredentialRow row : rows) {
            if (API_KEY.equals(row.credentialType())) {
                apiKey = cipher.decrypt(row.encryptedSecret());
                fingerprint = firstNonBlank(fingerprint, row.secretFingerprint());
            }
            if (SESSION_TOKEN.equals(row.credentialType())) {
                sessionToken = cipher.decrypt(row.encryptedSecret());
                sessionExpiresAt = row.expiresAt();
                lastRefreshedAt = row.lastRefreshedAt();
                fingerprint = firstNonBlank(fingerprint, row.secretFingerprint());
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
        jdbc.sql("""
                insert into spky_llm_credential (
                    credential_id, user_id, provider, credential_type, encrypted_secret,
                    secret_fingerprint, expires_at, last_refreshed_at, status, created_at, updated_at
                ) values (
                    :credentialId, :userId, :provider, :credentialType, :encryptedSecret,
                    :secretFingerprint, :expiresAt, :lastRefreshedAt, 'ACTIVE', now(), now()
                )
                on conflict (user_id, provider, credential_type) do update set
                    encrypted_secret = excluded.encrypted_secret,
                    secret_fingerprint = excluded.secret_fingerprint,
                    expires_at = excluded.expires_at,
                    last_refreshed_at = excluded.last_refreshed_at,
                    status = 'ACTIVE',
                    updated_at = now()
                """)
                .param("credentialId", UUID.randomUUID())
                .param("userId", USER_ID)
                .param("provider", provider.name())
                .param("credentialType", credentialType)
                .param("encryptedSecret", cipher.encrypt(secret))
                .param("secretFingerprint", SecretCipher.fingerprint(secret))
                .param("expiresAt", ts(expiresAt))
                .param("lastRefreshedAt", ts(lastRefreshedAt))
                .update();
    }

    private void deleteCredential(LlmProviderType provider, String credentialType) {
        jdbc.sql("""
                delete from spky_llm_credential
                where user_id = :userId
                  and provider = :provider
                  and credential_type = :credentialType
                """)
                .param("userId", USER_ID)
                .param("provider", provider.name())
                .param("credentialType", credentialType)
                .update();
    }

    private void ensureSystemUser() {
        jdbc.sql("""
                insert into spky_user_account (
                    user_id, username, display_name, status, created_at, updated_at
                ) values (
                    :userId, :username, 'System', 'ACTIVE', now(), now()
                )
                on conflict (username) do nothing
                """)
                .param("userId", USER_ID)
                .param("username", SystemUserIds.SYSTEM_USERNAME)
                .update();
    }

    private static String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private record CredentialRow(
            String credentialType,
            String encryptedSecret,
            String secretFingerprint,
            Instant expiresAt,
            Instant lastRefreshedAt
    ) {}
}
