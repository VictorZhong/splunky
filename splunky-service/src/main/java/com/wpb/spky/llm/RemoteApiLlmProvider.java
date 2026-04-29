package com.wpb.spky.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.config.SplunkyProperties;
import com.wpb.spky.llm.LlmCompletionResponse.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RemoteApiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(RemoteApiLlmProvider.class);

    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int maxCompletionTokens;
    private final RestClient http;

    public RemoteApiLlmProvider(ObjectMapper mapper, SplunkyProperties properties) {
        SplunkyProperties.RemoteProperties remote = properties.llmOrDefaults().remoteOrDefaults();
        this.mapper = mapper;
        this.baseUrl = trimTrailingSlash(nullIfBlank(remote.baseUrl()));
        this.apiKey = nullIfBlank(remote.apiKey());
        this.model = remote.modelOrDefault();
        this.maxCompletionTokens = remote.maxCompletionTokensOrDefault();
        this.http = RestClient.builder().build();
    }

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.REMOTE_API;
    }

    @Override
    public boolean isAvailable() {
        return baseUrl != null && apiKey != null;
    }

    @Override
    public LlmCompletionResponse complete(LlmCompletionRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Remote API LLM provider is not configured.");
        }

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
        log.debug("LLM Remote request: url={} provider={} model={} body={}",
                url, providerType(), model, toJson(body));
        String rawResponse;
        try {
            rawResponse = http.post()
                    .uri(url)
                    .headers(h -> {
                        h.setBearerAuth(apiKey);
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.debug("LLM Remote error response: url={} provider={} model={} status={} headers={} body={}",
                    url, providerType(), model, ex.getStatusCode().value(),
                    ex.getResponseHeaders(), ex.getResponseBodyAsString());
            throw ex;
        }
        log.debug("LLM Remote response: url={} provider={} model={} body={}",
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
        return new LlmCompletionResponse(LlmProviderType.REMOTE_API, model,
                extractText(message == null ? null : message.content()), toolCalls);
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

    private static String trimTrailingSlash(String value) {
        if (value == null) return null;
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

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
