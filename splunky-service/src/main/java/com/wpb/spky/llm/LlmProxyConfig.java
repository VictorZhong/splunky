package com.wpb.spky.llm;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public record LlmProxyConfig(
        String scheme,
        String host,
        int port,
        String username,
        String password
) {
    public static LlmProxyConfig disabled() {
        return new LlmProxyConfig("http", null, 0, null, null);
    }

    public static LlmProxyConfig parse(String proxyUrl) {
        if (proxyUrl == null || proxyUrl.isBlank()) return disabled();

        URI uri = URI.create(proxyUrl);
        String scheme = uri.getScheme() == null || uri.getScheme().isBlank() ? "http" : uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : defaultProxyPort(scheme);
        if (host == null || host.isBlank() || port <= 0) {
            throw new IllegalArgumentException(
                    "Invalid LLM proxy URL. Expected http://username:password@host:port.");
        }

        String username = null;
        String password = null;
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null && !rawUserInfo.isBlank()) {
            String[] parts = rawUserInfo.split(":", 2);
            username = percentDecode(parts[0]);
            password = parts.length > 1 ? percentDecode(parts[1]) : "";
        }
        return new LlmProxyConfig(scheme, host, port, username, password);
    }

    public boolean enabled() {
        return host != null && !host.isBlank() && port > 0;
    }

    public boolean hasCredentials() {
        return username != null && !username.isBlank() && password != null;
    }

    private static int defaultProxyPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static String percentDecode(String raw) {
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
