package com.wpb.spky.api;

import com.wpb.spky.llm.LlmCredentialDtos.LlmCredentialStatus;
import com.wpb.spky.llm.LlmCredentialStore;
import com.wpb.spky.llm.LlmProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm-credential")
@RequiredArgsConstructor
public class LlmCredentialController {

    private final LlmCredentialStore credentials;

    @GetMapping("/status")
    public LlmCredentialStatus status() {
        return credentials.status(LlmProviderType.COPILOT_PERSONAL);
    }
}
