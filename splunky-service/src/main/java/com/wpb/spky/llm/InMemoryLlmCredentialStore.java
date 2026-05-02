package com.wpb.spky.llm;

import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "splunky.persistence.enabled", havingValue = "false")
public class InMemoryLlmCredentialStore implements LlmCredentialStore {

    private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(25);

    private final ConcurrentMap<LlmProviderType, Credential> credentials = new ConcurrentHashMap<>();
    private final SplunkyProperties properties;

    @PostConstruct
    public void bootstrap() {
        SplunkyProperties.CopilotProperties copilot = properties.llmOrDefaults().copilotOrDefaults();
        bootstrapIfMissing(
                LlmProviderType.COPILOT_PERSONAL,
                copilot.bootstrapApiKey(),
                copilot.bootstrapSessionToken()
        );
        if (find(LlmProviderType.COPILOT_PERSONAL).isPresent()) {
            log.info("LLM credential store: Copilot bootstrap credential loaded into memory.");
        } else {
            log.info("LLM credential store: no Copilot bootstrap credentials configured.");
        }
    }

    @Override
    public Optional<Credential> find(LlmProviderType provider) {
        return Optional.ofNullable(credentials.get(provider));
    }

    @Override
    public LlmCredentialStatus status(LlmProviderType provider) {
        Credential credential = credentials.get(provider);
        boolean configured = credential != null && (credential.hasApiKey() || credential.hasSessionToken());
        boolean apiKeyConfigured = credential != null && credential.hasApiKey();
        boolean sessionTokenConfigured = credential != null && credential.hasSessionToken();
        String status = !configured
                ? "NOT_CONFIGURED"
                : (apiKeyConfigured || credential.hasFreshSessionToken(Instant.now())) ? "ACTIVE" : "EXPIRED";
        return new LlmCredentialStatus(
                configured,
                provider.name(),
                status,
                apiKeyConfigured,
                sessionTokenConfigured,
                credential == null ? null : credential.sessionTokenExpiresAt(),
                credential == null ? null : credential.updatedAt()
        );
    }

    @Override
    public void upsertSessionToken(LlmProviderType provider, String sessionToken, Instant expiresAt) {
        credentials.compute(provider, (ignored, current) -> new Credential(
                provider,
                current == null ? null : current.apiKey(),
                nullIfBlank(sessionToken),
                isBlank(sessionToken) ? null : resolvedExpiresAt(expiresAt),
                Instant.now()
        ));
    }

    @Override
    public void bootstrapIfMissing(LlmProviderType provider, String apiKey, String sessionToken) {
        if (isBlank(apiKey) && isBlank(sessionToken)) {
            return;
        }
        credentials.putIfAbsent(provider, new Credential(
                provider,
                nullIfBlank(apiKey),
                nullIfBlank(sessionToken),
                isBlank(sessionToken) ? null : Instant.now().plus(DEFAULT_SESSION_TTL),
                Instant.now()
        ));
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
