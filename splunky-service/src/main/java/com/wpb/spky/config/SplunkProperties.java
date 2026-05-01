package com.wpb.spky.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "splunky.splunk")
public record SplunkProperties(
        String scheme,
        String host,
        Integer port,
        Boolean trustAllSsl,
        Integer jobTtlSeconds,
        Integer maxAuthRetry,
        Integer resultRowLimit
) {

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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
