export type InvestigationInputType =
  | 'SPLUNK_URL'
  | 'CORRELATION_ID'
  | 'ERROR_RESPONSE'
  | 'API_NAME_OR_FIELD'
  | 'NATURAL_LANGUAGE'

export type InvestigationStatus = 'SUCCESS' | 'FAILED' | 'PARTIAL' | 'NO_RESULT'

export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN'

export type EventStatus = 'OK' | 'FAILED' | 'TIMEOUT' | 'WARNING' | 'INFERRED'

export interface Investigation {
  id: string
  activeRunId: string
  createdAt: string
  updatedAt: string
  input: InvestigationInput
  runs: InvestigationRunSummary[]
  activeResult: InvestigationResult
  conversation: ChatMessage[]
}

export interface InvestigationInput {
  rawText: string
  detectedTypes: InvestigationInputType[]
  timeRange: TimeRange
  apiName?: string
  correlationId?: string
}

export interface TimeRange {
  label: string
  from: string
  to: string
  timezone: TimezoneOption
}

export interface TimezoneOption {
  label: string
  offset: string
}

export interface InvestigationRunSummary {
  runId: string
  runNumber: number
  title: string
  userQuestion?: string
  action: FollowUpActionType
  summary: string
  createdAt: string
}

export interface InvestigationResult {
  investigationId: string
  runId: string
  runNumber: number
  context: InvestigationContext
  summary: DiagnosisSummary
  timeline: TimelineEvent[]
  serviceGraph: ServiceGraph
  sequence: SequenceViewModel
  downstreamCalls: DownstreamCall[]
  rawLogs: RawLogEntry[]
  queries: SplQueryRecord[]
  suggestedFollowUps: SuggestedFollowUp[]
}

export interface InvestigationContext {
  timeRange: TimeRange
  correlationId?: string
  apiName?: string
  lastRunAt: string
}

export interface DiagnosisSummary {
  status: InvestigationStatus
  finalHttpStatus?: number
  failurePoint?: string
  rootCauseHypothesis?: string
  confidence: Confidence
  logsFound: number
  affectedServices: string[]
  evidence: EvidenceItem[]
  recommendedActions: string[]
}

export interface EvidenceItem {
  id: string
  title: string
  description: string
  timestamp?: string
  service?: string
  relatedLogIds: string[]
  relatedTimelineEventIds: string[]
}

export interface TimelineEvent {
  id: string
  timestamp: string
  service: string
  eventType: string
  status: EventStatus
  durationMs?: number
  message: string
  relatedLogIds: string[]
  downstreamCallId?: string
}

export interface ServiceGraph {
  nodes: ServiceNode[]
  edges: ServiceEdge[]
}

export interface ServiceNode {
  id: string
  serviceName: string
  platform?: string
  status: EventStatus
  logCount: number
  errorCount: number
}

export interface ServiceEdge {
  id: string
  source: string
  target: string
  label: string
  operation?: string
  status: EventStatus
  latencyMs?: number
  evidenceType: 'CONFIRMED' | 'INFERRED'
  relatedLogIds: string[]
  downstreamCallId?: string
}

export interface SequenceViewModel {
  participants: SequenceParticipant[]
  messages: SequenceMessage[]
}

export interface SequenceParticipant {
  id: string
  label: string
  serviceName: string
}

export interface SequenceMessage {
  id: string
  from: string
  to: string
  label: string
  timestamp: string
  status: EventStatus
  durationMs?: number
  relatedLogIds: string[]
  downstreamCallId?: string
}

export interface DownstreamCall {
  id: string
  caller: string
  downstream: string
  operation: string
  endpoint?: string
  status: EventStatus
  latencyMs?: number
  errorMessage?: string
  relatedLogIds: string[]
}

export interface RawLogEntry {
  id: string
  timestamp: string
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  service: string
  eventType?: string
  message: string
  fields: Record<string, unknown>
  splunkUrl?: string
}

export interface SplQueryRecord {
  id: string
  templateName: string
  reason: string
  spl: string
  timeRange: TimeRange
  resultCount: number
  executionDurationMs: number
  status: 'SUCCESS' | 'FAILED'
  splunkUrl?: string
}

export type FollowUpActionType =
  | 'ANSWER_FROM_CURRENT_RESULT'
  | 'REFINE_ANALYSIS'
  | 'RERUN_QUERY'
  | 'NEW_INVESTIGATION'

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt: string
  actionType?: FollowUpActionType
  relatedRunId?: string
}

export interface SuggestedFollowUp {
  id: string
  label: string
  prompt: string
  actionType: FollowUpActionType
}

export interface StartInvestigationRequest {
  rawText: string
  selectedInputTypes: InvestigationInputType[]
  timeRange: TimeRange
  apiName?: string
}

export interface FollowUpRequest {
  prompt: string
}

export interface RequeryStatus {
  title: string
  changes: string[]
  queryType: string
}

export interface FollowUpResponse {
  investigation: Investigation
  actionType: FollowUpActionType
  requeryStatus?: RequeryStatus
}

export type DrawerType =
  | 'TIMELINE_EVENT'
  | 'SERVICE_NODE'
  | 'SERVICE_EDGE'
  | 'DOWNSTREAM_CALL'
  | 'RAW_LOG'
  | 'SPL_QUERY'
  | 'EVIDENCE'
  | 'SEQUENCE_MESSAGE'

export interface DrawerSelection {
  type: DrawerType
  id: string
}
