package com.wpb.spky.investigation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpb.spky.investigation.InvestigationDtos.DiagnosisSummary;
import com.wpb.spky.investigation.InvestigationDtos.EvidenceItem;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationInput;
import com.wpb.spky.llm.LlmCompletionRequest;
import com.wpb.spky.llm.LlmCompletionRequest.Message;
import com.wpb.spky.llm.LlmCompletionRequest.Role;
import com.wpb.spky.llm.LlmRouter;
import com.wpb.spky.splunk.SplunkSearchRequest;
import com.wpb.spky.splunk.SplunkSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestigationSummaryService {
    private static final int LLM_ROW_LIMIT = 30;
    private static final int FIELD_VALUE_LIMIT = 700;

    private final LlmRouter llmRouter;
    private final ObjectMapper mapper;

    public DiagnosisSummary summarize(InvestigationInput input, SplunkSearchRequest query,
                                      SplunkSearchResult result, Instant now) {
        SummaryModel model = llmRouter.currentIfAvailable()
                .map(provider -> {
                    try {
                        String content = provider.complete(new LlmCompletionRequest(
                                List.of(new Message(Role.SYSTEM, systemPrompt()),
                                        new Message(Role.USER, userPrompt(input, query, result))),
                                1400,
                                0.2
                        )).content();
                        return parseSummary(content);
                    } catch (Exception ex) {
                        log.warn("LLM investigation summary failed; using deterministic fallback: {}", ex.getMessage());
                        return fallback(input, query, result);
                    }
                })
                .orElseGet(() -> fallback(input, query, result));

        List<EvidenceItem> evidence = toEvidence(model.evidence(), input.apiName(), now, result.rows());
        return new DiagnosisSummary(
                firstNonBlank(model.status(), result.rows().isEmpty() ? "UNKNOWN" : "PARTIAL"),
                model.finalHttpStatus(),
                firstNonBlank(model.failurePoint(), input.apiName()),
                templatedHypothesis(model, query, result),
                firstNonBlank(model.confidence(), "UNKNOWN"),
                safeInt(result.resultCount()),
                safeList(model.affectedServices()),
                evidence,
                safeList(model.recommendedActions())
        );
    }

    private SummaryModel fallback(InvestigationInput input, SplunkSearchRequest query, SplunkSearchResult result) {
        List<String> evidence = new ArrayList<>();
        List<Map<String, Object>> previews = previewRows(result.rows(), 5);
        for (int i = 0; i < previews.size(); i++) {
            evidence.add("Row " + (i + 1) + ": " + previews.get(i));
        }
        if (evidence.isEmpty()) {
            evidence.add("No Splunk result rows were returned for the selected time range.");
        }

        String hypothesis = result.rows().isEmpty()
                ? "No matching Splunk rows were returned. The selected time range, query terms, or environment may need adjustment."
                : "Splunk returned evidence rows. Configure the LLM provider to produce a richer incident summary from these rows.";

        return new SummaryModel(
                result.rows().isEmpty() ? "UNKNOWN" : "PARTIAL",
                null,
                firstNonBlank(input.apiName(), "unknown"),
                hypothesis,
                result.rows().isEmpty() ? "LOW" : "UNKNOWN",
                List.of(firstNonBlank(input.apiName(), "unknown")),
                evidence,
                List.of("Verify the selected time range and timezone.",
                        "Open the Splunk job link if deeper row inspection is needed.",
                        "Configure the LLM provider for evidence-based summary wording.")
        );
    }

    private String templatedHypothesis(SummaryModel model, SplunkSearchRequest query, SplunkSearchResult result) {
        return """
                Scope: %s to %s
                Source: %s
                Result volume: %d rows returned, sid=%s
                Key findings: %s
                Error/status signals: %s
                Likely hypothesis: %s
                Confidence and limitations: %s
                """.formatted(
                query.earliest(),
                query.latest(),
                firstNonBlank(query.sourceUrl(), "Generated SPL"),
                result.resultCount(),
                firstNonBlank(result.sid(), "n/a"),
                joinOrDefault(model.evidence(), "No evidence rows highlighted."),
                firstNonBlank(model.failurePoint(), "No specific failure point identified."),
                firstNonBlank(model.rootCauseHypothesis(), "No hypothesis generated."),
                firstNonBlank(model.confidence(), "UNKNOWN")
        ).trim();
    }

    private List<EvidenceItem> toEvidence(List<String> evidence, String service, Instant now,
                                          List<Map<String, String>> rows) {
        List<String> items = safeList(evidence);
        List<EvidenceItem> result = new ArrayList<>();
        String timestamp = now.toString();
        for (int i = 0; i < Math.min(items.size(), 8); i++) {
            result.add(new EvidenceItem(
                    "ev-%03d".formatted(i + 1),
                    "Splunk evidence " + (i + 1),
                    items.get(i),
                    rowTime(rows, i, timestamp),
                    firstNonBlank(service, rowField(rows, i, "app"), rowField(rows, i, "appd"), rowField(rows, i, "service")),
                    List.of("log-%03d".formatted(i + 1)),
                    List.of()
            ));
        }
        return result;
    }

    private SummaryModel parseSummary(String content) throws JsonProcessingException {
        return mapper.readValue(extractJson(content), SummaryModel.class);
    }

    private String userPrompt(InvestigationInput input, SplunkSearchRequest query, SplunkSearchResult result)
            throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userInput", input.rawText());
        body.put("apiName", input.apiName());
        body.put("correlationId", input.correlationId());
        body.put("timeRange", input.timeRange());
        body.put("spl", query.spl());
        body.put("earliest", query.earliest());
        body.put("latest", query.latest());
        body.put("splunkUrl", firstNonBlank(query.sourceUrl(), result.splunkUrl()));
        body.put("sid", result.sid());
        body.put("resultCount", result.resultCount());
        body.put("rows", previewRows(result.rows(), LLM_ROW_LIMIT));
        return mapper.writeValueAsString(body);
    }

    private static String systemPrompt() {
        return """
                Summarize Splunk search results for API troubleshooting.
                Return JSON only with keys:
                status, finalHttpStatus, failurePoint, rootCauseHypothesis, confidence,
                affectedServices, evidence, recommendedActions.
                status should be SUCCESS, FAILED, PARTIAL, or UNKNOWN.
                confidence should be HIGH, MEDIUM, LOW, or UNKNOWN.
                Evidence and recommendations must be arrays of concise strings.
                Do not invent facts that are not supported by rows. Say when evidence is missing or inconclusive.
                The summary must cover: scope/time range, executed query/source, result volume, key findings,
                error/status signals, likely hypothesis, concrete evidence, next actions, confidence and limitations.
                """;
    }

    private static List<Map<String, Object>> previewRows(List<Map<String, String>> rows, int limit) {
        if (rows == null || rows.isEmpty()) return List.of();
        return rows.stream()
                .limit(limit)
                .map(InvestigationSummaryService::previewRow)
                .toList();
    }

    private static Map<String, Object> previewRow(Map<String, String> row) {
        Map<String, Object> preview = new LinkedHashMap<>();
        List<String> priority = List.of("_time", "time", "timestamp", "app", "appd", "service",
                "host", "source", "sourcetype", "level", "status", "statusCode",
                "error", "exception", "message", "_raw");
        for (String key : priority) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                preview.put(key, truncate(value));
            }
        }
        if (preview.isEmpty()) {
            row.entrySet().stream().limit(10).forEach(e -> preview.put(e.getKey(), truncate(e.getValue())));
        }
        return preview;
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

    private static String rowTime(List<Map<String, String>> rows, int index, String fallback) {
        if (rows == null || index >= rows.size()) return fallback;
        return firstNonBlank(rows.get(index).get("_time"), rows.get(index).get("time"),
                rows.get(index).get("timestamp"), fallback);
    }

    private static String rowField(List<Map<String, String>> rows, int index, String field) {
        if (rows == null || index >= rows.size()) return null;
        return rows.get(index).get(field);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(v -> v != null && !v.isBlank())
                .toList();
    }

    private static String joinOrDefault(List<String> values, String fallback) {
        List<String> safe = safeList(values);
        return safe.isEmpty() ? fallback : String.join("; ", safe);
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= FIELD_VALUE_LIMIT) return value;
        return value.substring(0, FIELD_VALUE_LIMIT) + "...";
    }

    private static int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SummaryModel(
            String status,
            Integer finalHttpStatus,
            String failurePoint,
            String rootCauseHypothesis,
            String confidence,
            List<String> affectedServices,
            List<String> evidence,
            List<String> recommendedActions
    ) {}
}
