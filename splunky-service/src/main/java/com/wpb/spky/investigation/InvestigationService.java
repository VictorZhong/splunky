package com.wpb.spky.investigation;

import com.wpb.spky.investigation.InvestigationDtos.ChatMessage;
import com.wpb.spky.investigation.InvestigationDtos.DiagnosisSummary;
import com.wpb.spky.investigation.InvestigationDtos.DownstreamCall;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpRequest;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpResponse;
import com.wpb.spky.investigation.InvestigationDtos.Investigation;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationContext;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationInput;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationResult;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationRunSummary;
import com.wpb.spky.investigation.InvestigationDtos.RawLogEntry;
import com.wpb.spky.investigation.InvestigationDtos.RequeryStatus;
import com.wpb.spky.investigation.InvestigationDtos.SequenceViewModel;
import com.wpb.spky.investigation.InvestigationDtos.ServiceGraph;
import com.wpb.spky.investigation.InvestigationDtos.SplQueryRecord;
import com.wpb.spky.investigation.InvestigationDtos.StartInvestigationRequest;
import com.wpb.spky.investigation.InvestigationDtos.SuggestedFollowUp;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.investigation.InvestigationDtos.TimezoneOption;
import com.wpb.spky.persistence.AuditEventStore;
import com.wpb.spky.session.UserSession;
import com.wpb.spky.splunk.ParsedSplunkUrl;
import com.wpb.spky.splunk.SplunkSearchRequest;
import com.wpb.spky.splunk.SplunkSearchResult;
import com.wpb.spky.splunk.SplunkSearcher;
import com.wpb.spky.splunk.SplunkUrlParser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Service
public class InvestigationService {

    private static final Pattern UUID_LIKE = Pattern.compile(
            "\\b([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}|[a-z]{2,8}-\\d{2,8}|abc-123|def-456)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLICIT_CORRELATION = Pattern.compile(
            "correlation(?:Id| ID| id)?[:=\\s]+([a-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_NAME = Pattern.compile("\\b([a-z][a-z0-9-]+(?:sapi|api|service))\\b",
            Pattern.CASE_INSENSITIVE);

    private final ConcurrentMap<String, Investigation> investigations = new ConcurrentHashMap<>();
    private final SplunkQueryPlanner queryPlanner;
    private final SplunkUrlParser splunkUrls;
    private final SplunkSearcher splunkSearcher;
    private final InvestigationSummaryService summaries;
    private final AuditEventStore auditEvents;

    public InvestigationService(SplunkQueryPlanner queryPlanner, SplunkUrlParser splunkUrls,
                                SplunkSearcher splunkSearcher, InvestigationSummaryService summaries,
                                AuditEventStore auditEvents) {
        this.queryPlanner = queryPlanner;
        this.splunkUrls = splunkUrls;
        this.splunkSearcher = splunkSearcher;
        this.summaries = summaries;
        this.auditEvents = auditEvents;
    }

    public Investigation start(UserSession session, StartInvestigationRequest request) {
        Investigation investigation = execute(session, request, 1, "Initial investigation",
                "ANSWER_FROM_CURRENT_RESULT", null, null);
        investigations.put(investigation.id(), investigation);
        return investigation;
    }

    public Investigation get(String investigationId) {
        Investigation investigation = investigations.get(investigationId);
        if (investigation == null) {
            throw new NoSuchElementException("Investigation not found: " + investigationId);
        }
        return investigation;
    }

    public FollowUpResponse followUp(UserSession session, String investigationId, FollowUpRequest request) {
        String prompt = request.normalizedPrompt();
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Follow-up prompt must not be blank.");
        }

        Investigation current = get(investigationId);
        String action = classifyFollowUp(prompt);
        auditEvents.record(session, "FOLLOW_UP_RECEIVED", "INVESTIGATION", null,
                "Follow-up received", Map.of("investigationId", investigationId, "action", action));
        ChatMessage userMessage = message("USER", prompt, action, current.activeRunId());

        if ("NEW_INVESTIGATION".equals(action)) {
            Investigation next = start(session,
                    new StartInvestigationRequest(prompt, detectInputTypes(prompt), current.input().timeRange(), null));
            Investigation updated = appendConversation(next, List.of(userMessage));
            investigations.put(updated.id(), updated);
            return new FollowUpResponse(updated, action, null);
        }

        if ("RERUN_QUERY".equals(action)) {
            Investigation rerun = rerun(session, current, prompt);
            ChatMessage assistant = message("ASSISTANT", rerun.activeResult().summary().rootCauseHypothesis(),
                    action, rerun.activeRunId());
            Investigation updated = appendConversation(rerun, List.of(userMessage, assistant));
            investigations.put(investigationId, updated);
            return new FollowUpResponse(updated, action, new RequeryStatus("Re-running investigation",
                    List.of("Splunk query refreshed for the current session", "Summary regenerated from latest rows"),
                    "splunk search"));
        }

        String answer = current.activeResult().summary().rootCauseHypothesis();
        ChatMessage assistant = message("ASSISTANT", answer, action, current.activeRunId());
        Investigation updated = appendConversation(current, List.of(userMessage, assistant));
        investigations.put(investigationId, updated);
        return new FollowUpResponse(updated, action, null);
    }

    private Investigation rerun(UserSession session, Investigation current, String prompt) {
        int runNumber = current.runs().size() + 1;
        StartInvestigationRequest request = new StartInvestigationRequest(
                current.input().rawText() + "\nFollow-up: " + prompt,
                current.input().detectedTypes(),
                current.input().timeRange(),
                current.input().apiName()
        );
        Investigation rerun = execute(session, request, runNumber, "Refined investigation",
                "RERUN_QUERY", current.id(), current.createdAt());
        List<InvestigationRunSummary> runs = new ArrayList<>(current.runs());
        runs.add(rerun.runs().get(0));
        return new Investigation(current.id(), rerun.activeRunId(), current.createdAt(), rerun.updatedAt(),
                current.input(), runs, rerun.activeResult(), new ArrayList<>(current.conversation()));
    }

    private Investigation execute(UserSession session, StartInvestigationRequest request, int runNumber,
                                  String runTitle, String action, String existingInvestigationId,
                                  String existingCreatedAt) {
        TimeRange timeRange = defaultTimeRangeIfMissing(request.timeRange());
        Optional<ParsedSplunkUrl> parsedUrl = splunkUrls.parseFromText(request.rawText());
        if (parsedUrl.isPresent()) {
            timeRange = mergeUrlTimeRange(timeRange, parsedUrl.get());
        }

        List<String> detectedTypes = request.selectedInputTypes() == null || request.selectedInputTypes().isEmpty()
                ? detectInputTypes(request.rawText())
                : request.selectedInputTypes();
        if (parsedUrl.isPresent() && !detectedTypes.contains("SPLUNK_URL")) {
            detectedTypes = new ArrayList<>(detectedTypes);
            detectedTypes.add("SPLUNK_URL");
        }

        String correlationId = extractCorrelationId(request.rawText());
        String apiName = firstNonBlank(request.apiName(), inferApiName(request.rawText()), "unknown-api");
        Instant now = Instant.now();

        InvestigationInput input = new InvestigationInput(request.rawText(), List.copyOf(detectedTypes),
                timeRange, apiName, correlationId);
        SearchExecution execution = executeSplunk(session, input, parsedUrl);
        DiagnosisSummary summary = summaries.summarize(input, execution.query(), execution.result(), now);

        String investigationId = firstNonBlank(existingInvestigationId, id("inv"));
        String runId = id("run");
        String timestamp = now.truncatedTo(ChronoUnit.MILLIS).toString();
        InvestigationResult result = createResult(investigationId, runId, runNumber, input, timestamp,
                execution.query(), execution.result(), summary);
        InvestigationRunSummary run = new InvestigationRunSummary(runId, runNumber, runTitle,
                request.rawText(), action, summary.rootCauseHypothesis(), timestamp);

        Investigation investigation = new Investigation(
                investigationId,
                runId,
                firstNonBlank(existingCreatedAt, timestamp),
                timestamp,
                input,
                List.of(run),
                result,
                new ArrayList<>(List.of(
                        message("USER", request.rawText(), null, runId),
                        message("ASSISTANT", summary.rootCauseHypothesis(), null, runId)
                ))
        );
        auditEvents.record(session, runNumber == 1 ? "INVESTIGATION_CREATED" : "INVESTIGATION_RERUN",
                "INVESTIGATION", null, "Investigation run completed",
                Map.of("investigationId", investigationId, "runId", runId,
                        "resultCount", execution.result().resultCount()));
        return investigation;
    }

    private SearchExecution executeSplunk(UserSession session, InvestigationInput input,
                                          Optional<ParsedSplunkUrl> parsedUrl) {
        if (parsedUrl.isPresent()) {
            ParsedSplunkUrl url = parsedUrl.get();
            if (url.hasSid()) {
                SplunkSearchResult result = splunkSearcher.resultsForSid(session, url.sid(), url.url());
                SplunkSearchRequest query = new SplunkSearchRequest(
                        firstNonBlank(url.spl(), "sid=" + url.sid()),
                        firstNonBlank(url.earliest(), input.timeRange().from()),
                        firstNonBlank(url.latest(), input.timeRange().to()),
                        "Splunk URL job import",
                        url.url()
                );
                return new SearchExecution(query, result);
            }
            SplunkSearchRequest query = queryPlanner.fromUrlQuery(url.spl(), url.earliest(), url.latest(),
                    url.url(), input.timeRange());
            return new SearchExecution(query, splunkSearcher.search(session, query));
        }

        SplunkSearchRequest query = queryPlanner.plan(input.rawText(), input.timeRange());
        return new SearchExecution(query, splunkSearcher.search(session, query));
    }

    private InvestigationResult createResult(String investigationId, String runId, int runNumber,
                                             InvestigationInput input, String timestamp,
                                             SplunkSearchRequest query, SplunkSearchResult splunk,
                                             DiagnosisSummary summary) {
        return new InvestigationResult(investigationId, runId, runNumber,
                new InvestigationContext(input.timeRange(), input.correlationId(), input.apiName(), timestamp),
                summary,
                List.of(),
                new ServiceGraph(List.of(), List.of()),
                new SequenceViewModel(List.of(), List.of()),
                List.<DownstreamCall>of(),
                rawLogs(splunk),
                List.of(new SplQueryRecord("spl-001", "AI_OR_URL_SPLUNK_SEARCH",
                        query.reason(), query.spl(), input.timeRange(), safeInt(splunk.resultCount()),
                        safeInt(splunk.executionDurationMs()), "SUCCESS", splunk.splunkUrl())),
                suggestedFollowUps());
    }

    private List<RawLogEntry> rawLogs(SplunkSearchResult splunk) {
        List<Map<String, String>> rows = splunk.rows() == null ? List.of() : splunk.rows();
        List<RawLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), 50); i++) {
            Map<String, String> row = rows.get(i);
            String id = "log-%03d".formatted(i + 1);
            entries.add(new RawLogEntry(
                    id,
                    firstNonBlank(row.get("_time"), row.get("time"), row.get("timestamp")),
                    firstNonBlank(row.get("level"), row.get("severity"), "INFO"),
                    firstNonBlank(row.get("service"), row.get("app"), row.get("appd"), row.get("host"), "unknown"),
                    firstNonBlank(row.get("eventType"), row.get("sourcetype"), "splunk_result"),
                    truncate(firstNonBlank(row.get("message"), row.get("_raw"), row.toString()), 1200),
                    previewFields(row),
                    splunk.splunkUrl()
            ));
        }
        return entries;
    }

    private static Map<String, Object> previewFields(Map<String, String> row) {
        Map<String, Object> fields = new LinkedHashMap<>();
        row.entrySet().stream().limit(40)
                .forEach(e -> fields.put(e.getKey(), truncate(e.getValue(), 1200)));
        return fields;
    }

    private static TimeRange mergeUrlTimeRange(TimeRange selected, ParsedSplunkUrl url) {
        if (isBlank(url.earliest()) && isBlank(url.latest())) return selected;
        return new TimeRange("From Splunk URL",
                firstNonBlank(url.earliest(), selected.from()),
                firstNonBlank(url.latest(), selected.to()),
                selected.timezone());
    }

    private static List<SuggestedFollowUp> suggestedFollowUps() {
        return List.of(
                new SuggestedFollowUp("follow-evidence", "Show evidence",
                        "What evidence supports this?", "ANSWER_FROM_CURRENT_RESULT"),
                new SuggestedFollowUp("follow-summary", "Incident summary",
                        "Generate an incident summary.", "REFINE_ANALYSIS")
        );
    }

    private static Investigation appendConversation(Investigation investigation, List<ChatMessage> messages) {
        List<ChatMessage> conversation = new ArrayList<>(investigation.conversation());
        conversation.addAll(messages);
        return new Investigation(investigation.id(), investigation.activeRunId(), investigation.createdAt(),
                Instant.now().toString(), investigation.input(), investigation.runs(),
                investigation.activeResult(), conversation);
    }

    private static ChatMessage message(String role, String content, String actionType, String relatedRunId) {
        return new ChatMessage(id("msg"), role, content, Instant.now().toString(), actionType, relatedRunId);
    }

    private static String classifyFollowUp(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("new search") || lower.contains("new investigation")) return "NEW_INVESTIGATION";
        if (lower.contains("expand") || lower.contains("similar") || lower.contains("re-run")
                || lower.contains("rerun") || lower.contains("last 1 hour")) {
            return "RERUN_QUERY";
        }
        if (lower.contains("summary") || lower.contains("incident") || lower.contains("write")) {
            return "REFINE_ANALYSIS";
        }
        return "ANSWER_FROM_CURRENT_RESULT";
    }

    private static List<String> detectInputTypes(String rawText) {
        String value = rawText == null ? "" : rawText.trim();
        String lower = value.toLowerCase();
        Set<String> types = new LinkedHashSet<>();
        if (value.startsWith("{") || value.startsWith("[")) types.add("ERROR_RESPONSE");
        if (lower.contains("splunk") || lower.contains("/app/search")) types.add("SPLUNK_URL");
        if (lower.contains("correlation") || UUID_LIKE.matcher(value).find()) types.add("CORRELATION_ID");
        if (Pattern.compile("\\b(api|sapi|service|endpoint)\\b", Pattern.CASE_INSENSITIVE).matcher(value).find()) {
            types.add("API_NAME_OR_FIELD");
        }
        if (types.isEmpty() || lower.contains("why") || lower.contains("find")
                || lower.contains("check") || lower.contains("what")) {
            types.add("NATURAL_LANGUAGE");
        }
        return List.copyOf(types);
    }

    private static String extractCorrelationId(String rawText) {
        if (rawText == null) return null;
        var explicit = EXPLICIT_CORRELATION.matcher(rawText);
        if (explicit.find()) return explicit.group(1);
        var uuidLike = UUID_LIKE.matcher(rawText);
        return uuidLike.find() ? uuidLike.group(1) : null;
    }

    private static String inferApiName(String rawText) {
        if (rawText == null) return null;
        var match = API_NAME.matcher(rawText);
        return match.find() ? match.group(1).toLowerCase() : null;
    }

    private static TimeRange defaultTimeRangeIfMissing(TimeRange provided) {
        if (provided != null) return provided;
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.MINUTES);
        return new TimeRange("Last 30 min", from.toString(), to.toString(), new TimezoneOption("HKT", "+08:00"));
    }

    private static int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SearchExecution(SplunkSearchRequest query, SplunkSearchResult result) {}
}
