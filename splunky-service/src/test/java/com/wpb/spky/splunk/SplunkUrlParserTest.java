package com.wpb.spky.splunk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SplunkUrlParserTest {

    private final SplunkUrlParser parser = new SplunkUrlParser();

    @Test
    void extractsSearchAndTimeRangeFromSplunkUrl() {
        ParsedSplunkUrl parsed = parser.parseFromText(
                "open https://digital-splunk-search.hk.zzzz/en-US/app/search/search?q=search%20index%3Dmain%20abc-123&earliest=-15m&latest=now"
        ).orElseThrow();

        assertThat(parsed.spl()).isEqualTo("index=main abc-123");
        assertThat(parsed.earliest()).isEqualTo("-15m");
        assertThat(parsed.latest()).isEqualTo("now");
    }

    @Test
    void extractsSidFromSplunkUrlFragment() {
        ParsedSplunkUrl parsed = parser.parseFromText(
                "https://digital-splunk-search.hk.zzzz/en-US/app/search/search#/jobs?sid=admin__search__123"
        ).orElseThrow();

        assertThat(parsed.sid()).isEqualTo("admin__search__123");
    }

    @Test
    void ignoresNonSplunkUrls() {
        assertThat(parser.parseFromText("check https://example.com/api/payments")).isEmpty();
    }
}
