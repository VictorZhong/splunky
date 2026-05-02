package com.wpb.spky.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "splunky.splunk")
public record SplunkProperties(
        String scheme,
        String host,
        Integer port,
        Boolean trustAllSsl,
        Integer jobTtlSeconds,
        Integer maxAuthRetry,
        Integer resultRowLimit,
        String defaultEnvironment,
        Map<String, String> environments
) {

    private static final String ENV_DEV = "DEV";
    private static final String ENV_PROD_ON_PREM = "PROD_ON_PREM";
    private static final String ENV_PROD_AWS = "PROD_AWS";

    public String schemeOrDefault() {
        return isBlank(scheme) ? "https" : scheme;
    }

    public String hostOrDefault() {
        return isBlank(host) ? "digital-splunk-search.hk.zzzz" : host;
    }

    public int portOrDefault() {
        return port == null ? 8089 : port;
    }

    public boolean trustAllSslOrDefault() {
        return trustAllSsl == null || trustAllSsl;
    }

    public int jobTtlSecondsOrDefault() {
        return jobTtlSeconds == null ? 1800 : jobTtlSeconds;
    }

    public int maxAuthRetryOrDefault() {
        return maxAuthRetry == null ? 3 : maxAuthRetry;
    }

    public int resultRowLimitOrDefault() {
        return resultRowLimit == null ? 0 : resultRowLimit;
    }

    public String defaultEnvironmentOrDefault() {
        String env = canonicalEnvironment(defaultEnvironment);
        return isBlank(env) ? ENV_DEV : env;
    }

    public Map<String, String> environmentsOrDefault() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ENV_DEV, defaultUrl());
        map.put(ENV_PROD_ON_PREM, defaultUrl());
        map.put(ENV_PROD_AWS, defaultUrl());

        if (environments != null) {
            environments.forEach((key, value) -> {
                String normalizedKey = canonicalEnvironment(key);
                if (!isBlank(normalizedKey) && !isBlank(value)) {
                    map.put(normalizedKey, value.trim());
                }
            });
        }

        return Map.copyOf(map);
    }

    public SplunkEndpoint endpointForEnvironment(String rawEnvironment) {
        String environment = canonicalEnvironment(rawEnvironment);
        Map<String, String> configured = environmentsOrDefault();
        String resolvedEnvironment = configured.containsKey(environment)
                ? environment
                : defaultEnvironmentOrDefault();
        String rawUrl = configured.getOrDefault(resolvedEnvironment, defaultUrl());

        URI uri = URI.create(rawUrl.contains("://") ? rawUrl : "https://" + rawUrl);
        String resolvedScheme = isBlank(uri.getScheme()) ? schemeOrDefault() : uri.getScheme();
        String resolvedHost = isBlank(uri.getHost()) ? hostOrDefault() : uri.getHost();
        int resolvedPort = uri.getPort() > 0 ? uri.getPort() : portOrDefault();

        return new SplunkEndpoint(
                resolvedEnvironment,
                rawUrl,
                resolvedScheme,
                resolvedHost,
                resolvedPort
        );
    }

    public record SplunkEndpoint(
            String environment,
            String rawUrl,
            String scheme,
            String host,
            int port
    ) {}

    private String defaultUrl() {
        return schemeOrDefault() + "://" + hostOrDefault() + ":" + portOrDefault();
    }

    private static String canonicalEnvironment(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        return raw.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
