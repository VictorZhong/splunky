package com.wpb.spky.investigation;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public final class InvestigationDtos {

    private InvestigationDtos() {}

    public record StartInvestigationRequest(
            @NotBlank String rawText,
            List<String> selectedInputTypes,
            TimeRange timeRange,
            String apiName
    ) {}

    public record FollowUpRequest(
            String prompt,
            String message
    ) {
        public String normalizedPrompt() {
            return !isBlank(prompt) ? prompt : message;
        }
    }

    public record FollowUpResponse(
            Investigation investigation,
            String actionType,
            RequeryStatus requeryStatus
    ) {}

    public record RequeryStatus(
            String title,
            List<String> changes,
            String queryType
    ) {}

    public record Investigation(
            String id,
            String activeRunId,
            String createdAt,
            String updatedAt,
            InvestigationInput input,
            List<InvestigationRunSummary> runs,
            InvestigationResult activeResult,
            List<ChatMessage> conversation
    ) {}

    public record InvestigationInput(
            String rawText,
            List<String> detectedTypes,
            TimeRange timeRange,
            String apiName,
            String correlationId
    ) {}

    public record TimeRange(
            String label,
            String from,
            String to,
            TimezoneOption timezone
    ) {}

    public record TimezoneOption(
            String label,
            String offset
    ) {}

    public record InvestigationRunSummary(
            String runId,
            int runNumber,
            String title,
            String userQuestion,
            String action,
            String summary,
            String createdAt
    ) {}

    public record InvestigationResult(
            String investigationId,
            String runId,
            int runNumber,
            InvestigationContext context,
            DiagnosisSummary summary,
            List<TimelineEvent> timeline,
            ServiceGraph serviceGraph,
            SequenceViewModel sequence,
            List<DownstreamCall> downstreamCalls,
            List<RawLogEntry> rawLogs,
            List<SplQueryRecord> queries,
            List<SuggestedFollowUp> suggestedFollowUps
    ) {}

    public record InvestigationContext(
            TimeRange timeRange,
            String correlationId,
            String apiName,
            String lastRunAt
    ) {}

    public record DiagnosisSummary(
            String status,
            Integer finalHttpStatus,
            String failurePoint,
            String rootCauseHypothesis,
            String confidence,
            int logsFound,
            List<String> affectedServices,
            List<EvidenceItem> evidence,
            List<String> recommendedActions
    ) {}

    public record EvidenceItem(
            String id,
            String title,
            String description,
            String timestamp,
            String service,
            List<String> relatedLogIds,
            List<String> relatedTimelineEventIds
    ) {}

    public record TimelineEvent(
            String id,
            String timestamp,
            String service,
            String eventType,
            String status,
            Integer durationMs,
            String message,
            List<String> relatedLogIds,
            String downstreamCallId
    ) {}

    public record ServiceGraph(
            List<ServiceNode> nodes,
            List<ServiceEdge> edges
    ) {}

    public record ServiceNode(
            String id,
            String serviceName,
            String platform,
            String status,
            int logCount,
            int errorCount
    ) {}

    public record ServiceEdge(
            String id,
            String source,
            String target,
            String label,
            String operation,
            String status,
            Integer latencyMs,
            String evidenceType,
            List<String> relatedLogIds,
            String downstreamCallId
    ) {}

    public record SequenceViewModel(
            List<SequenceParticipant> participants,
            List<SequenceMessage> messages
    ) {}

    public record SequenceParticipant(
            String id,
            String label,
            String serviceName
    ) {}

    public record SequenceMessage(
            String id,
            String from,
            String to,
            String label,
            String timestamp,
            String status,
            Integer durationMs,
            List<String> relatedLogIds,
            String downstreamCallId
    ) {}

    public record DownstreamCall(
            String id,
            String caller,
            String downstream,
            String operation,
            String endpoint,
            String status,
            Integer latencyMs,
            String errorMessage,
            List<String> relatedLogIds
    ) {}

    public record RawLogEntry(
            String id,
            String timestamp,
            String level,
            String service,
            String eventType,
            String message,
            Map<String, Object> fields,
            String splunkUrl
    ) {}

    public record SplQueryRecord(
            String id,
            String templateName,
            String reason,
            String spl,
            TimeRange timeRange,
            int resultCount,
            int executionDurationMs,
            String status,
            String splunkUrl
    ) {}

    public record ChatMessage(
            String id,
            String role,
            String content,
            String createdAt,
            String actionType,
            String relatedRunId
    ) {}

    public record SuggestedFollowUp(
            String id,
            String label,
            String prompt,
            String actionType
    ) {}

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
