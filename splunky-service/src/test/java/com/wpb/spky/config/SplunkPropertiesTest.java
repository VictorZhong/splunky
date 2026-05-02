package com.wpb.spky.config;

import com.wpb.spky.config.SplunkProperties.SplunkEndpoint;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SplunkPropertiesTest {

    @Test
    void resolvesConfiguredEnvironmentByCode() {
        SplunkProperties properties = properties();

        SplunkEndpoint endpoint = properties.endpointForEnvironment("PROD_AWS");

        assertThat(endpoint.environment()).isEqualTo("PROD_AWS");
        assertThat(endpoint.host()).isEqualTo("splunk-aws.example.com");
        assertThat(endpoint.port()).isEqualTo(8089);
        assertThat(endpoint.scheme()).isEqualTo("https");
    }

    @Test
    void resolvesConfiguredEnvironmentByDisplayText() {
        SplunkProperties properties = properties();

        SplunkEndpoint endpoint = properties.endpointForEnvironment("Prod - On-prem");

        assertThat(endpoint.environment()).isEqualTo("PROD_ON_PREM");
        assertThat(endpoint.host()).isEqualTo("splunk-onprem.example.com");
    }

    @Test
    void fallsBackToDefaultEnvironmentWhenUnknown() {
        SplunkProperties properties = properties();

        SplunkEndpoint endpoint = properties.endpointForEnvironment("NOT_EXIST");

        assertThat(endpoint.environment()).isEqualTo("DEV");
        assertThat(endpoint.host()).isEqualTo("digital-splunk-search.hk.zzzz");
    }

    private static SplunkProperties properties() {
        return new SplunkProperties(
                "https",
                "digital-splunk-search.hk.zzzz",
                8089,
                true,
                1800,
                3,
                0,
                "DEV",
                Map.of(
                        "DEV", "https://digital-splunk-search.hk.zzzz:8089",
                        "PROD_ON_PREM", "https://splunk-onprem.example.com:8089",
                        "PROD_AWS", "https://splunk-aws.example.com:8089"
                )
        );
    }
}
