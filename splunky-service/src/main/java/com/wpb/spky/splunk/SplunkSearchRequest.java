package com.wpb.spky.splunk;

public record SplunkSearchRequest(
        String spl,
        String earliest,
        String latest,
        String reason,
        String sourceUrl
) {}
