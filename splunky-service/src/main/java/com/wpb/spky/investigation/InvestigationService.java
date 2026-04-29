package com.wpb.spky.investigation;

import com.wpb.spky.investigation.InvestigationDtos.ChatMessage;
import com.wpb.spky.investigation.InvestigationDtos.DiagnosisSummary;
import com.wpb.spky.investigation.InvestigationDtos.DownstreamCall;
import com.wpb.spky.investigation.InvestigationDtos.EvidenceItem;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpRequest;
import com.wpb.spky.investigation.InvestigationDtos.FollowUpResponse;
import com.wpb.spky.investigation.InvestigationDtos.Investigation;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationContext;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationInput;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationResult;
import com.wpb.spky.investigation.InvestigationDtos.InvestigationRunSummary;
import com.wpb.spky.investigation.InvestigationDtos.RawLogEntry;
import com.wpb.spky.investigation.InvestigationDtos.RequeryStatus;
import com.wpb.spky.investigation.InvestigationDtos.SequenceMessage;
import com.wpb.spky.investigation.InvestigationDtos.SequenceParticipant;
import com.wpb.spky.investigation.InvestigationDtos.SequenceViewModel;
import com.wpb.spky.investigation.InvestigationDtos.ServiceEdge;
import com.wpb.spky.investigation.InvestigationDtos.ServiceGraph;
import com.wpb.spky.investigation.InvestigationDtos.ServiceNode;
import com.wpb.spky.investigation.InvestigationDtos.SplQueryRecord;
import com.wpb.spky.investigation.InvestigationDtos.StartInvestigationRequest;
import com.wpb.spky.investigation.InvestigationDtos.SuggestedFollowUp;
import com.wpb.spky.investigation.InvestigationDtos.TimeRange;
import com.wpb.spky.investigation.InvestigationDtos.TimezoneOption;
import com.wpb.spky.investigation.InvestigationDtos.TimelineEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    public Investigation start(StartInvestigationRequest request) {
        TimeRange timeRange = defaultTimeRangeIfMissing(request.timeRange());
        List<String> detectedTypes = request.selectedInputTypes() == null || request.selectedInputTypes().isEmpty()
                ? detectInputTypes(request.rawText())
                : request.selectedInputTypes();
        String correlationId = extractCorrelationId(request.rawText());
        String apiName = firstNonBlank(request.apiName(), inferApiName(request.rawText()), "payment-sapi");
        Instant now = Instant.now();

        String investigationId = id("inv");
        String runId = id("run");
        InvestigationInput input = new InvestigationInput(request.rawText(), detectedTypes, timeRange, apiName, correlationId);
        InvestigationResult result = createResult(investigationId, runId, 1, input, now, 0);
        InvestigationRunSummary run = new InvestigationRunSummary(runId, 1, "Initial investigation",
                request.rawText(), "ANSWER_FROM_CURRENT_RESULT", result.summary().rootCauseHypothesis(), now.toString());

        Investigation investigation = new Investigation(
                investigationId,
                runId,
                now.toString(),
                now.toString(),
                input,
                List.of(run),
                result,
                new ArrayList<>(List.of(
                        message("USER", request.rawText(), null, runId),
                        message("ASSISTANT", result.summary().rootCauseHypothesis(), null, runId)
                ))
        );
        investigations.put(investigationId, investigation);
        return investigation;
    }

    public Investigation get(String investigationId) {
        Investigation investigation = investigations.get(investigationId);
        if (investigation == null) {
            throw new NoSuchElementException("Investigation not found: " + investigationId);
        }
        return investigation;
    }

    public FollowUpResponse followUp(String investigationId, FollowUpRequest request) {
        String prompt = request.normalizedPrompt();
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Follow-up prompt must not be blank.");
        }

        Investigation current = get(investigationId);
        String action = classifyFollowUp(prompt);
        ChatMessage userMessage = message("USER", prompt, action, current.activeRunId());

        if ("NEW_INVESTIGATION".equals(action)) {
            Investigation next = start(new StartInvestigationRequest(prompt, detectInputTypes(prompt), current.input().timeRange(), null));
            Investigation updated = appendConversation(next, List.of(userMessage));
            investigations.put(updated.id(), updated);
            return new FollowUpResponse(updated, action, null);
        }

        if ("RERUN_QUERY".equals(action)) {
            Investigation rerun = rerun(current, prompt);
            ChatMessage assistant = message("ASSISTANT",
                    "I expanded the search scope and refreshed the active result. Splunk execution is still stubbed in this backend scaffold.",
                    action, rerun.activeRunId());
            Investigation updated = appendConversation(rerun, List.of(userMessage, assistant));
            investigations.put(investigationId, updated);
            return new FollowUpResponse(updated, action, new RequeryStatus("Re-running investigation",
                    List.of("Time range expanded for scaffold response", "Splunk client integration pending"),
                    "controlled template search"));
        }

        String answer = "REFINE_ANALYSIS".equals(action)
                ? "Incident summary: the scaffold result keeps the evidence-first shape ready for real Splunk and LLM analysis."
                : "Based on the current scaffold evidence, payment-sapi is the likely investigation focus. Real Splunk evidence will replace this placeholder.";
        ChatMessage assistant = message("ASSISTANT", answer, action, current.activeRunId());
        Investigation updated = appendConversation(current, List.of(userMessage, assistant));
        investigations.put(investigationId, updated);
        return new FollowUpResponse(updated, action, null);
    }

    private Investigation rerun(Investigation current, String prompt) {
        int runNumber = current.runs().size() + 1;
        String runId = id("run");
        Instant now = Instant.now();
        InvestigationResult result = createResult(current.id(), runId, runNumber, current.input(), now, 12);
        List<InvestigationRunSummary> runs = new ArrayList<>(current.runs());
        runs.add(new InvestigationRunSummary(runId, runNumber, "Refined investigation", prompt,
                "RERUN_QUERY", result.summary().rootCauseHypothesis(), now.toString()));
        return new Investigation(current.id(), runId, current.createdAt(), now.toString(), current.input(),
                runs, result, new ArrayList<>(current.conversation()));
    }

    private InvestigationResult createResult(String investigationId, String runId, int runNumber,
                                             InvestigationInput input, Instant now, int similarTimeouts) {
        String timestamp = now.truncatedTo(ChronoUnit.MILLIS).toString();
        String apiName = firstNonBlank(input.apiName(), "payment-sapi");
        String correlationId = firstNonBlank(input.correlationId(), "abc-123");

        List<EvidenceItem> evidence = new ArrayList<>(List.of(
                new EvidenceItem("ev-001", "Controlled query plan selected",
                        "The backend selected approved Splunk query templates rather than allowing arbitrary SPL.",
                        timestamp, apiName, List.of("log-001"), List.of("tl-001")),
                new EvidenceItem("ev-002", "LLM analysis pending real evidence",
                        "The LLM provider abstraction is wired, but this scaffold does not invoke it until Splunk evidence is available.",
                        timestamp, apiName, List.of("log-002"), List.of("tl-002"))
        ));
        if (similarTimeouts > 0) {
            evidence.add(new EvidenceItem("ev-003", "Expanded search placeholder",
                    similarTimeouts + " similar timeout events would be summarized here after Splunk integration.",
                    timestamp, apiName, List.of("log-003"), List.of("tl-003")));
        }

        DiagnosisSummary summary = new DiagnosisSummary("PARTIAL", null, apiName,
                "Backend scaffold is ready; real root-cause analysis needs Splunk query execution and evidence normalization.",
                "UNKNOWN", similarTimeouts > 0 ? similarTimeouts : 2,
                List.of(apiName, "splunk", "llm-provider"),
                evidence,
                List.of("Wire Splunk REST client", "Replace placeholder evidence with normalized Splunk events",
                        "Invoke LLM summarizer with ranked evidence"));

        List<TimelineEvent> timeline = List.of(
                new TimelineEvent("tl-001", timestamp, apiName, "query_planned", "OK", null,
                        "Approved query templates selected", List.of("log-001"), null),
                new TimelineEvent("tl-002", timestamp, apiName, "analysis_stubbed", "INFERRED", null,
                        "Evidence normalization and LLM summarization are scaffolded", List.of("log-002"), null)
        );
        ServiceGraph serviceGraph = new ServiceGraph(
                List.of(
                        new ServiceNode("node-api", apiName, "SHP", "WARNING", 2, 0),
                        new ServiceNode("node-splunk", "splunk-api", null, "UNKNOWN", 0, 0),
                        new ServiceNode("node-llm", "llm-provider", null, "UNKNOWN", 0, 0)
                ),
                List.of(
                        new ServiceEdge("edge-splunk", apiName, "splunk-api", "search jobs",
                                "POST /services/search/jobs", "UNKNOWN", null, "INFERRED", List.of(), null),
                        new ServiceEdge("edge-llm", apiName, "llm-provider", "summarize evidence",
                                "chat/completions", "UNKNOWN", null, "INFERRED", List.of(), null)
                )
        );
        SequenceViewModel sequence = new SequenceViewModel(
                List.of(
                        new SequenceParticipant("user", "User", "user"),
                        new SequenceParticipant("backend", "Splunky Backend", "splunky-service"),
                        new SequenceParticipant("splunk", "Splunk", "splunk-api"),
                        new SequenceParticipant("llm", "LLM", "llm-provider")
                ),
                List.of(
                        new SequenceMessage("seq-001", "user", "backend", "Start investigation", timestamp,
                                "OK", null, List.of(), null),
                        new SequenceMessage("seq-002", "backend", "splunk", "Execute controlled SPL templates", timestamp,
                                "INFERRED", null, List.of(), null),
                        new SequenceMessage("seq-003", "backend", "llm", "Summarize ranked evidence", timestamp,
                                "INFERRED", null, List.of(), null)
                )
        );
        List<DownstreamCall> downstreamCalls = List.of(
                new DownstreamCall("call-splunk", "splunky-service", "splunk-api",
                        "POST /services/search/jobs", "/services/search/jobs", "UNKNOWN", null,
                        "Splunk REST client is pending", List.of()),
                new DownstreamCall("call-llm", "splunky-service", "llm-provider",
                        "POST /chat/completions", "/chat/completions", "UNKNOWN", null,
                        "LLM provider is wired but not invoked by placeholder investigation", List.of())
        );
        List<RawLogEntry> rawLogs = List.of(
                new RawLogEntry("log-001", timestamp, "INFO", apiName, "query_planned",
                        "Placeholder only. Raw Splunk results must remain transient.",
                        Map.of("correlationId", correlationId, "rawPersisted", false), null)
        );
        List<SplQueryRecord> queries = List.of(
                new SplQueryRecord("spl-001", "FIND_EVENTS_BY_CORRELATION_ID",
                        "Initial request correlation search",
                        "index=${index} correlationId=\"" + correlationId + "\" | sort _time",
                        input.timeRange(), similarTimeouts > 0 ? similarTimeouts : 0, 0, "SUCCESS", null)
        );

        return new InvestigationResult(investigationId, runId, runNumber,
                new InvestigationContext(input.timeRange(), correlationId, apiName, timestamp),
                summary, timeline, serviceGraph, sequence, downstreamCalls, rawLogs, queries, suggestedFollowUps());
    }

    private static List<SuggestedFollowUp> suggestedFollowUps() {
        return List.of(
                new SuggestedFollowUp("follow-similar", "Find similar errors",
                        "Find similar timeout errors.", "RERUN_QUERY"),
                new SuggestedFollowUp("follow-expand", "Expand to last 1 hour",
                        "Expand to last 1 hour.", "RERUN_QUERY"),
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

    private static String id(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
