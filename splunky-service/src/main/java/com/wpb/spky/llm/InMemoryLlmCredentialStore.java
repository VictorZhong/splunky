package com.wpb.spky.llm;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryLlmCredentialStore implements LlmCredentialStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLlmCredentialStore.class);
    private static final Duration BOOTSTRAP_SESSION_TTL = Duration.ofMinutes(25);

    private final ConcurrentMap<LlmProviderType, Credential> credentials = new ConcurrentHashMap<>();
    private final SplunkyProperties properties;

    public InMemoryLlmCredentialStore(SplunkyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void bootstrap() {
        SplunkyProperties.CopilotProperties copilot = properties.llmOrDefaults().copilotOrDefaults();
        boolean hasApiKey = !isBlank(copilot.bootstrapApiKey());
        boolean hasSessionToken = !isBlank(copilot.bootstrapSessionToken());
        if (!hasApiKey && !hasSessionToken) {
            log.info("LLM credential store: no Copilot bootstrap credentials configured.");
            return;
        }

        Instant now = Instant.now();
        Credential credential = new Credential(
                LlmProviderType.COPILOT_PERSONAL,
                nullIfBlank(copilot.bootstrapApiKey()),
                nullIfBlank(copilot.bootstrapSessionToken()),
                hasSessionToken ? now.plus(BOOTSTRAP_SESSION_TTL) : null,
                hasSessionToken ? now : null,
                fingerprint(firstNonBlank(copilot.bootstrapApiKey(), copilot.bootstrapSessionToken()))
        );
        credentials.putIfAbsent(LlmProviderType.COPILOT_PERSONAL, credential);
        log.info("LLM credential store: Copilot bootstrap credential loaded into memory.");
    }

    @Override
    public Optional<Credential> find(LlmProviderType provider) {
        return Optional.ofNullable(credentials.get(provider));
    }

    @Override
    public LlmCredentialStatus status(LlmProviderType provider) {
        Credential credential = credentials.get(provider);
        if (credential == null || (!credential.hasApiKey() && isBlank(credential.sessionToken()))) {
            return new LlmCredentialStatus(false, provider.name(), "NOT_CONFIGURED", null, null, null);
        }
        Instant now = Instant.now();
        String status = credential.hasFreshSessionToken(now) || credential.hasApiKey() ? "ACTIVE" : "EXPIRED";
        return new LlmCredentialStatus(true, provider.name(), status, credential.sessionTokenExpiresAt(),
                credential.lastRefreshedAt(), credential.secretFingerprint());
    }

    @Override
    public LlmCredentialStatus upsertApiKey(LlmProviderType provider, String apiKey) {
        if (isBlank(apiKey)) throw new IllegalArgumentException("LLM API key must not be blank.");
        credentials.compute(provider, (ignored, current) -> new Credential(
                provider,
                apiKey,
                current == null ? null : current.sessionToken(),
                current == null ? null : current.sessionTokenExpiresAt(),
                current == null ? null : current.lastRefreshedAt(),
                fingerprint(apiKey)
        ));
        return status(provider);
    }

    @Override
    public LlmCredentialStatus upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt) {
        Instant resolvedExpiresAt = isBlank(sessionToken)
                ? null
                : expiresAt == null ? Instant.now().plus(BOOTSTRAP_SESSION_TTL) : expiresAt;
        credentials.compute(provider, (ignored, current) -> new Credential(
                provider,
                current == null ? null : current.apiKey(),
                nullIfBlank(sessionToken),
                resolvedExpiresAt,
                isBlank(sessionToken) ? current == null ? null : current.lastRefreshedAt() : Instant.now(),
                current == null
                        ? fingerprint(sessionToken)
                        : firstNonBlank(current.secretFingerprint(), fingerprint(sessionToken))
        ));
        return status(provider);
    }

    private static String fingerprint(String secret) {
        if (isBlank(secret)) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private static String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private static String nullIfBlank(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
