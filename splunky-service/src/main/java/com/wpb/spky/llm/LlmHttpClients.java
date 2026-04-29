package com.wpb.spky.llm;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

final class LlmHttpClients {

    private LlmHttpClients() {}

    static RestClient restClient(LlmProxyConfig proxy) {
        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(60))
                .setProxyPreferredAuthSchemes(List.of(StandardAuthScheme.BASIC))
                .build();

        var builder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries();

        if (proxy.enabled()) {
            HttpHost proxyHost = new HttpHost(proxy.scheme(), proxy.host(), proxy.port());
            builder.setProxy(proxyHost);
            if (proxy.hasCredentials()) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(proxyHost),
                        new UsernamePasswordCredentials(proxy.username(), proxy.password().toCharArray())
                );
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(builder.build());
        return RestClient.builder().requestFactory(factory).build();
    }
}
