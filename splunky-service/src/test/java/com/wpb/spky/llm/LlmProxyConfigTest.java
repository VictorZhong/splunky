package com.wpb.spky.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
