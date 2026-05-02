package com.wpb.spky.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProxyConfigTest {

    @Test
    void parsesAuthenticatedProxyUrl() {
        LlmProxyConfig proxy = LlmProxyConfig.parse("http://user%40corp:p%2Bss%3Aword@proxy.local:8080");

        assertThat(proxy.enabled()).isTrue();
        assertThat(proxy.scheme()).isEqualTo("http");
        assertThat(proxy.host()).isEqualTo("proxy.local");
        assertThat(proxy.port()).isEqualTo(8080);
        assertThat(proxy.username()).isEqualTo("user@corp");
        assertThat(proxy.password()).isEqualTo("p+ss:word");
    }

    @Test
    void defaultsHttpProxyPort() {
        LlmProxyConfig proxy = LlmProxyConfig.parse("http://proxy.local");

        assertThat(proxy.enabled()).isTrue();
        assertThat(proxy.port()).isEqualTo(80);
        assertThat(proxy.hasCredentials()).isFalse();
    }

    @Test
    void parsesBase64EncodedProxyCredentials() {
        LlmProxyConfig proxy = LlmProxyConfig.parse("http://base64:dXNlckBjb3JwOnArc3M6d29yZA==@proxy.local:8080");

        assertThat(proxy.enabled()).isTrue();
        assertThat(proxy.host()).isEqualTo("proxy.local");
        assertThat(proxy.port()).isEqualTo(8080);
        assertThat(proxy.username()).isEqualTo("user@corp");
        assertThat(proxy.password()).isEqualTo("p+ss:word");
        assertThat(proxy.hasCredentials()).isTrue();
    }

    @Test
    void throwsOnInvalidBase64Credentials() {
        assertThatThrownBy(() -> LlmProxyConfig.parse("http://base64:not-base64@proxy.local:8080"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid base64 proxy credentials");
    }
}
