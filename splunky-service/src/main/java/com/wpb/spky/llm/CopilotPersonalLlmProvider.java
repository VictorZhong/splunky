package com.wpb.spky.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.llm.LlmCompletionResponse.ToolCall;
import com.wpb.spky.llm.LlmCredentialStore.Credential;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CopilotPersonalLlmProvider implements LlmProvider {

    private static final String API_VERSION = "2025-04-01";
    private static final String EDITOR_PLUGIN_VERSION = "copilot.vim/1.16.0";
    private static final String USER_AGENT = "GithubCopilot/1.155.0";
    private static final Duration ASSUMED_SESSION_TTL = Duration.ofMinutes(25);

    private final LlmCredentialStore credentials;
    private final ObjectMapper mapper;
    private final String bootstrapApiKey;
    private final String bootstrapSessionToken;
    private final String model;
    private final int maxCompletionTokens;
    private final String editorVersion;
    private final String baseUrl;
    private final String tokenUrl;
    private final LlmProxyConfig proxy;
    private final RestClient http;

    public CopilotPersonalLlmProvider(
            LlmCredentialStore credentials,
            ObjectMapper mapper,
            SplunkyProperties properties) {
        SplunkyProperties.CopilotProperties copilot = properties.llmOrDefaults().copilotOrDefaults();
        this.credentials = credentials;
        this.mapper = mapper;
        this.bootstrapApiKey = nullIfBlank(copilot.bootstrapApiKey());
        this.bootstrapSessionToken = nullIfBlank(copilot.bootstrapSessionToken());
        this.model = copilot.modelOrDefault();
        this.maxCompletionTokens = copilot.maxCompletionTokensOrDefault();
        this.editorVersion = copilot.editorVersionOrDefault();
        this.baseUrl = trimTrailingSlash(requireConfiguredUrl("splunky.llm.copilot.base-url", copilot.baseUrl()));
        this.tokenUrl = requireConfiguredUrl("splunky.llm.copilot.token-url", copilot.tokenUrl());
        this.proxy = LlmProxyConfig.parse(copilot.proxyUrl());
        this.http = LlmHttpClients.restClient(proxy);
    }

    @PostConstruct
    public void logConfiguration() {
        if (bootstrapApiKey != null || bootstrapSessionToken != null) {
            credentials.bootstrapIfMissing(
                    LlmProviderType.COPILOT_PERSONAL,
                    bootstrapApiKey,
                    bootstrapSessionToken
            );
        }

        Credential credential = credentials.find(LlmProviderType.COPILOT_PERSONAL).orElse(null);
        if (credential == null) {
            log.info("Copilot Personal: no credential row in spky_llm_credential; provider unavailable until configured.");
        } else if (!credential.hasApiKey() && !credential.hasSessionToken()) {
            log.info("Copilot Personal: credential row exists but has no api_key/session_token.");
        } else {
            log.info("Copilot Personal: credentials loaded from spky_llm_credential.");
        }
        if (proxy.enabled()) {
            log.info("Copilot Personal: outbound proxy configured at {}://{}:{} auth={}",
                    proxy.scheme(), proxy.host(), proxy.port(), proxy.hasCredentials());
        }
    }

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.COPILOT_PERSONAL;
    }

    @Override
    public boolean isAvailable() {
        return credentials.find(LlmProviderType.COPILOT_PERSONAL)
                .map(c -> c.hasApiKey() || c.hasFreshSessionToken(Instant.now()))
                .orElse(false);
    }

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        try {
            String token = ensureSessionToken();
            return sendCompletion(request, token);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401) {
                credentials.upsertSessionToken(LlmProviderType.COPILOT_PERSONAL, null, null);
                try {
                    return sendCompletion(request, ensureSessionToken());
                } catch (HttpClientErrorException retryEx) {
                    throw new RestClientException("Copilot chat completions failed: "
                            + retryEx.getStatusCode(), retryEx);
                }
            }
            throw new RestClientException("Copilot chat completions failed: " + ex.getStatusCode(), ex);
        }
    }

    private LlmCompletionResponse sendCompletion(LlmCompletionRequest request, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("max_completion_tokens", request.maxTokens() == null ? maxCompletionTokens : request.maxTokens());
        if (request.temperature() != null) body.put("temperature", request.temperature());
        body.put("messages", ChatCompletionPayloads.messages(request.messages()));
        if (request.tools() != null && !request.tools().isEmpty()) {
            body.put("tools", ChatCompletionPayloads.tools(request.tools()));
        }
        if (request.toolChoice() != null && !request.toolChoice().isBlank()) {
            body.put("tool_choice", request.toolChoice());
        }

        String url = baseUrl + "/chat/completions";
        log.debug("LLM Copilot request: url={} provider={} model={} body={}",
                url, providerType(), model, toJson(body));
        String rawResponse;
        try {
            rawResponse = http.post()
                    .uri(url)
                    .headers(h -> copilotHeaders(h, token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.debug("LLM Copilot error response: url={} provider={} model={} status={} headers={} body={}",
                    url, providerType(), model, ex.getStatusCode().value(),
                    ex.getResponseHeaders(), ex.getResponseBodyAsString());
            throw ex;
        }
        log.debug("LLM Copilot response: url={} provider={} model={} body={}",
                url, providerType(), model, rawResponse);

        ChatCompletionResponse resp = readJson(rawResponse, ChatCompletionResponse.class);
        ChoiceMessage message = resp != null && resp.choices() != null && !resp.choices().isEmpty()
                ? resp.choices().get(0).message()
                : null;
        List<ToolCall> toolCalls = message != null && message.toolCalls() != null
                ? message.toolCalls().stream()
                    .filter(tc -> tc.function() != null && tc.function().name() != null)
                    .map(tc -> new ToolCall(tc.id(), tc.function().name(), tc.function().arguments()))
                    .toList()
                : List.of();
        String content = extractText(message == null ? null : message.content());
        return new LlmCompletionResponse(LlmProviderType.COPILOT_PERSONAL, model, content, toolCalls);
    }

    private String ensureSessionToken() {
        Credential credential = credentials.find(LlmProviderType.COPILOT_PERSONAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Copilot Personal provider is not configured. Insert a row into spky_llm_credential or bootstrap with LLM_API_KEY."));
        Instant now = Instant.now();
        if (credential.hasFreshSessionToken(now)) return credential.sessionToken();
        if (!credential.hasApiKey()) {
            throw new IllegalStateException("Cannot refresh Copilot session token: API key is empty.");
        }

        log.debug("LLM Copilot token request: url={} proxyEnabled={} proxyAuth={}",
                tokenUrl, proxy.enabled(), proxy.hasCredentials());
        TokenResponse token;
        try {
            String rawResponse = http.get()
                    .uri(tokenUrl)
                    .headers(h -> githubHeaders(h, credential.apiKey()))
                    .retrieve()
                    .body(String.class);
            log.debug("LLM Copilot token response: url={} body={}", tokenUrl, rawResponse);
            token = readJson(rawResponse, TokenResponse.class);
        } catch (HttpClientErrorException ex) {
            log.debug("LLM Copilot token error response: url={} status={} headers={} body={}",
                    tokenUrl, ex.getStatusCode().value(), ex.getResponseHeaders(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 401) {
                throw new RestClientException(
                        "Copilot token refresh failed with 401 Unauthorized. Check the configured LLM API key.", ex);
            }
            if (ex.getStatusCode().value() == 407) {
                throw new RestClientException(
                        "Copilot token refresh failed with 407 Proxy Authentication Required. "
                                + "Check LLM_PROXY_URL and percent-encode special characters.", ex);
            }
            throw ex;
        } catch (ResourceAccessException ex) {
            if (isAuthenticationRetryFailure(ex)) {
                throw new RestClientException(
                        "Copilot token refresh failed because HTTP authentication was rejected repeatedly. "
                                + "Check proxy credentials and LLM API key.", ex);
            }
            throw ex;
        }

        if (token == null || token.token() == null || token.token().isBlank()) {
            throw new RestClientException("GitHub Copilot token response did not include a token.");
        }
        Instant expiresAt = token.expiresAt() != null
                ? Instant.ofEpochSecond(token.expiresAt())
                : Instant.now().plus(ASSUMED_SESSION_TTL);
        credentials.upsertSessionToken(LlmProviderType.COPILOT_PERSONAL, token.token(), expiresAt);
        log.debug("LLM Copilot session token refreshed; expiresAt={} tokenChars={}",
                expiresAt, token.token().length());
        return token.token();
    }

    private void copilotHeaders(HttpHeaders h, String sessionToken) {
        h.set(HttpHeaders.AUTHORIZATION, "Bearer " + sessionToken);
        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        h.set("copilot-integration-id", "vscode-chat");
        h.set("editor-version", "vscode/" + editorVersion);
        h.set("editor-plugin-version", EDITOR_PLUGIN_VERSION);
        h.set(HttpHeaders.USER_AGENT, USER_AGENT);
        h.set("openai-intent", "conversation-panel");
        h.set("x-github-api-version", API_VERSION);
        h.set("x-vscode-user-agent-library-version", "electron-fetch");
        h.set("x-initiator", "agent");
    }

    private void githubHeaders(HttpHeaders h, String apiKey) {
        h.set(HttpHeaders.AUTHORIZATION, "token " + apiKey);
        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        h.set("editor-version", "vscode/" + editorVersion);
        h.set("editor-plugin-version", EDITOR_PLUGIN_VERSION);
        h.set(HttpHeaders.USER_AGENT, USER_AGENT);
        h.set("x-github-api-version", API_VERSION);
        h.set("x-vscode-user-agent-library-version", "electron-fetch");
    }

    private <T> T readJson(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return mapper.readValue(raw, type);
        } catch (JsonProcessingException ex) {
            throw new RestClientException("Failed to parse LLM response as " + type.getSimpleName(), ex);
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private static boolean isAuthenticationRetryFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("too many authentication attempts")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String extractText(Object content) {
        if (content instanceof String s) return s;
        if (content instanceof List<?> parts) {
            StringBuilder text = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> map) {
                    Object value = map.get("text");
                    if (value != null) text.append(value);
                }
            }
            return text.toString();
        }
        return "";
    }

    private static String requireConfiguredUrl(String propertyName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured.");
        }
        return value;
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(String token, @JsonAlias("expires_at") Long expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChoiceMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChoiceMessage(String role, Object content, @JsonAlias("tool_calls") List<RawToolCall> toolCalls) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawToolCall(String id, RawFunction function) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawFunction(String name, String arguments) {}
}
