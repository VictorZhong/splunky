package com.wpb.spky.investigation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.llm.LlmCompletionRequest;
import com.wpb.spky.llm.LlmCompletionRequest.Message;
import com.wpb.spky.llm.LlmCompletionRequest.Role;
import com.wpb.spky.llm.LlmRouter;
import com.wpb.spky.splunk.SplunkSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SplunkQueryPlanner {

    private static final Logger log = LoggerFactory.getLogger(SplunkQueryPlanner.class);
    private static final Pattern CORRELATION = Pattern.compile(
            "\\b([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}|[a-z]{2,8}-\\d{2,8}|abc-123|def-456)\\b",
            Pattern.CASE_INSENSITIVE);

    private final LlmRouter llmRouter;
    private final ObjectMapper mapper;

    public SplunkQueryPlanner(LlmRouter llmRouter, ObjectMapper mapper) {
        this.llmRouter = llmRouter;
        this.mapper = mapper;
    }

    public SplunkSearchRequest plan(String rawText, TimeRange timeRange) {
        return llmRouter.currentIfAvailable()
                .map(provider -> {
                    try {
                        String content = provider.complete(new LlmCompletionRequest(
                                List.of(new Message(Role.SYSTEM, systemPrompt()),
                                        new Message(Role.USER, userPrompt(rawText, timeRange))),
                                700,
                                0.1
                        )).content();
                        PlannedSplunkQuery planned = parsePlannedQuery(content);
                        return toSearchRequest(planned, timeRange, null);
                    } catch (Exception ex) {
                        log.warn("LLM Splunk query planning failed; using heuristic fallback: {}", ex.getMessage());
                        return fallback(rawText, timeRange);
                    }
                })
                .orElseGet(() -> fallback(rawText, timeRange));
    }

    public SplunkSearchRequest fromUrlQuery(String spl, String earliest, String latest, String url, TimeRange fallbackRange) {
        return toSearchRequest(new PlannedSplunkQuery(spl, earliest, latest, "Splunk URL query"), fallbackRange, url);
    }

    private SplunkSearchRequest fallback(String rawText, TimeRange timeRange) {
        String target = extractCorrelation(rawText);
        String reason = target == null ? "Heuristic free-text search" : "Heuristic correlation-id search";
        String searchValue = target == null ? rawText : target;
        String spl = "index=* " + quoteForSplunk(searchValue)
                + " | table _time,host,source,sourcetype,_raw | head 200";
        return new SplunkSearchRequest(spl, earliest(timeRange), latest(timeRange), reason, null);
    }

    private SplunkSearchRequest toSearchRequest(PlannedSplunkQuery planned, TimeRange timeRange, String sourceUrl) {
        String spl = stripSearchPrefix(planned.spl());
        if (spl == null || spl.isBlank()) {
            throw new IllegalArgumentException("Planned Splunk search is blank.");
        }
        return new SplunkSearchRequest(
                spl,
                firstNonBlank(planned.earliest(), earliest(timeRange)),
                firstNonBlank(planned.latest(), latest(timeRange)),
                firstNonBlank(planned.reason(), "AI planned Splunk query"),
                sourceUrl
        );
    }

    private PlannedSplunkQuery parsePlannedQuery(String content) throws Exception {
        String json = extractJson(content);
        return mapper.readValue(json, PlannedSplunkQuery.class);
    }

    private static String systemPrompt() {
        return """
                You generate bounded read-only Splunk SPL for an API troubleshooting tool.
                Return JSON only with keys: spl, earliest, latest, reason.
                Do not include a leading "search " prefix in spl.
                Use only read-only SPL. Never use delete, collect, outputlookup, outputcsv, sendemail, script, map, rest, or dbxquery.
                Keep result volume bounded with table/stats/head unless the user provided an explicit SPL.
                Prefer fields useful for summary: _time, host, source, sourcetype, app/appd/service, level, status/statusCode, error/exception, _raw.
                """;
    }

    private static String userPrompt(String rawText, TimeRange timeRange) {
        String tz = timeRange == null || timeRange.timezone() == null ? "" : timeRange.timezone().offset();
        return """
                User input:
                %s

                Selected time:
                label=%s
                from=%s
                to=%s
                timezoneOffset=%s
                """.formatted(
                rawText == null ? "" : rawText,
                timeRange == null ? "" : timeRange.label(),
                timeRange == null ? "" : timeRange.from(),
                timeRange == null ? "" : timeRange.to(),
                tz == null ? "" : tz
        );
    }

    private static String extractJson(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        int fencedStart = trimmed.indexOf("```");
        if (fencedStart >= 0) {
            int firstNewLine = trimmed.indexOf('\n', fencedStart);
            int fencedEnd = trimmed.indexOf("```", firstNewLine + 1);
            if (firstNewLine >= 0 && fencedEnd > firstNewLine) {
                trimmed = trimmed.substring(firstNewLine + 1, fencedEnd).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return start >= 0 && end > start ? trimmed.substring(start, end + 1) : trimmed;
    }

    private static String stripSearchPrefix(String spl) {
        if (spl == null) return null;
        String value = spl.trim();
        return value.regionMatches(true, 0, "search ", 0, "search ".length())
                ? value.substring("search ".length()).trim()
                : value;
    }

    private static String extractCorrelation(String rawText) {
        if (rawText == null) return null;
        var matcher = CORRELATION.matcher(rawText);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String quoteForSplunk(String value) {
        String safe = value == null ? "" : value.trim();
        safe = safe.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    private static String earliest(TimeRange timeRange) {
        return firstNonBlank(timeRange == null ? null : timeRange.from(), "-30m");
    }

    private static String latest(TimeRange timeRange) {
        return firstNonBlank(timeRange == null ? null : timeRange.to(), "now");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }
}
