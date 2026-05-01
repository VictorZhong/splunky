package com.wpb.spky.splunk;

import java.util.List;
import java.util.Map;

public record SplunkSearchResult(
        String sid,
        long eventCount,
        long resultCount,
        float runDurationSeconds,
        long executionDurationMs,
        List<Map<String, String>> rows,
        String splunkUrl
) {}
