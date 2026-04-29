package com.wpb.spky.llm;

import com.wpb.spky.config.SplunkyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LlmRouter {

    private static final Logger log = LoggerFactory.getLogger(LlmRouter.class);

    private final Map<LlmProviderType, LlmProvider> providers = new EnumMap<>(LlmProviderType.class);
    private final LlmProviderType primary;
    private final LlmProviderType fallback;

    public LlmRouter(List<LlmProvider> providerBeans, SplunkyProperties properties) {
        for (LlmProvider provider : providerBeans) {
            providers.put(provider.providerType(), provider);
        }
        SplunkyProperties.LlmProperties llm = properties.llmOrDefaults();
        this.primary = LlmProviderType.parse(llm.primaryProvider(), LlmProviderType.COPILOT_PERSONAL);
        this.fallback = LlmProviderType.parse(llm.fallbackProvider(), LlmProviderType.REMOTE_API);
    }

    public LlmProvider current() {
        Optional<LlmProvider> available = currentIfAvailable();
        if (available.isPresent()) return available.get();
        log.warn("No LLM provider is available; returning primary {} so calls fail with a configuration error.", primary);
        LlmProvider provider = providers.get(primary);
        if (provider != null) return provider;
        return providers.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No LLM providers are registered."));
    }

    public Optional<LlmProvider> currentIfAvailable() {
        LlmProvider primaryProvider = providers.get(primary);
        if (primaryProvider != null && primaryProvider.isAvailable()) return Optional.of(primaryProvider);

        LlmProvider fallbackProvider = providers.get(fallback);
        if (fallbackProvider != null && fallbackProvider.isAvailable()) {
            log.warn("Primary LLM provider {} unavailable, using fallback {}", primary, fallback);
            return Optional.of(fallbackProvider);
        }
        return Optional.empty();
    }
}
