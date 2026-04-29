package com.wpb.spky.api;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import com.wpb.spky.llm.LlmCredentialDtos.UpsertLlmCredentialRequest;
import com.wpb.spky.llm.LlmCredentialStore;
import com.wpb.spky.llm.LlmProviderType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm-credential")
public class LlmCredentialController {

    private final LlmCredentialStore credentials;

    public LlmCredentialController(LlmCredentialStore credentials) {
        this.credentials = credentials;
    }

    @GetMapping("/status")
    public LlmCredentialStatus status() {
        return credentials.status(LlmProviderType.COPILOT_PERSONAL);
    }

    @PutMapping
    public LlmCredentialStatus upsert(@Valid @RequestBody UpsertLlmCredentialRequest request) {
        LlmProviderType provider = LlmProviderType.parse(request.provider(), LlmProviderType.COPILOT_PERSONAL);
        String type = request.credentialType().trim().toUpperCase();
        if ("API_KEY".equals(type)) {
            return credentials.upsertApiKey(provider, request.secret());
        }
        if ("ACCESS_TOKEN".equals(type) || "SESSION_TOKEN".equals(type)) {
            return credentials.upsertSessionToken(provider, request.secret(), request.expiresAt());
        }
        throw new IllegalArgumentException("Unsupported LLM credential type: " + request.credentialType());
    }
}
