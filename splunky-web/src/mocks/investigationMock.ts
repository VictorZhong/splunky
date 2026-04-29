import dayjs from 'dayjs'
import {
  buildTimeRange,
  detectInputType,
  extractCorrelationId,
  inferApiName,
} from '../features/investigation/utils/inputDetection'
import type {
  ChatMessage,
  DiagnosisSummary,
  DownstreamCall,
  EventStatus,
  EvidenceItem,
  FollowUpActionType,
  Investigation,
  InvestigationInput,
  InvestigationResult,
  ServiceGraph,
  SequenceViewModel,
  SplQueryRecord,
  StartInvestigationRequest,
  SuggestedFollowUp,
  TimelineEvent,
} from '../features/investigation/types'
import { createRawLogsMock } from './logsMock'

const mockUserInput = 'correlation id abc-123'

function createId(prefix: string) {
  return `${prefix}-${Math.random().toString(36).slice(2, 8)}`
}

function createChatMessage(
  role: ChatMessage['role'],
  content: string,
  actionType?: FollowUpActionType,
  relatedRunId?: string,
): ChatMessage {
  return {
    id: createId('msg'),
    role,
    content,
    createdAt: dayjs().toISOString(),
    actionType,
    relatedRunId,
  }
}

function createInput(request: StartInvestigationRequest): InvestigationInput {
  const detectedType = detectInputType(request.rawText)
  const correlationId =
    extractCorrelationId(request.rawText) ??
    (detectedType === 'CORRELATION_ID' ? request.rawText.trim() : 'abc-123')

  return {
    rawText: request.rawText,
    detectedType,
    environment: request.environment,
    timeRange: buildTimeRange(request.timeRangeLabel),
    apiName: request.apiName || inferApiName(request.rawText) || 'payment-sapi',
    market: request.market && request.market !== 'All' ? request.market : 'HK',
    correlationId,
  }
}

function createSuggestedFollowUps(): SuggestedFollowUp[] {
  return [
    {
      id: 'follow-similar',
      label: 'Find similar errors',
      prompt: 'Find similar timeout errors.',
      actionType: 'RERUN_QUERY',
    },
    {
      id: 'follow-expand',
      label: 'Expand to last 1 hour',
      prompt: 'Expand to last 1 hour.',
      actionType: 'RERUN_QUERY',
    },
    {
      id: 'follow-failed',
      label: 'Failed downstream only',
      prompt: 'Search failed downstream only.',
      actionType: 'RERUN_QUERY',
    },
    {
      id: 'follow-explain',
      label: 'Explain failure point',
      prompt: 'Explain the failure point.',
      actionType: 'ANSWER_FROM_CURRENT_RESULT',
    },
    {
      id: 'follow-incident',
      label: 'Incident summary',
      prompt: 'Generate an incident summary.',
      actionType: 'REFINE_ANALYSIS',
    },
    {
      id: 'follow-evidence',
      label: 'Show raw evidence',
      prompt: 'What evidence supports this?',
      actionType: 'ANSWER_FROM_CURRENT_RESULT',
    },
  ]
}

function createEvidence(similarTimeouts: number): EvidenceItem[] {
  const evidence: EvidenceItem[] = [
    {
      id: 'ev-001',
      title: 'Inbound request reached payment-sapi',
      description:
        'Gateway and payment-sapi logs show the request was received and correlated correctly.',
      timestamp: dayjs().second(3).millisecond(160).toISOString(),
      service: 'payment-sapi',
      relatedLogIds: ['log-001', 'log-002'],
      relatedTimelineEventIds: ['tl-001'],
    },
    {
      id: 'ev-002',
      title: 'Validation completed before downstream calls',
      description:
        'Schema and business validation passed in 330ms, so the failure was not caused by request validation.',
      timestamp: dayjs().second(3).millisecond(450).toISOString(),
      service: 'payment-sapi',
      relatedLogIds: ['log-003'],
      relatedTimelineEventIds: ['tl-002'],
    },
    {
      id: 'ev-003',
      title: 'HUB propose call timed out',
      description:
        'payment-sapi called hub-payment-propose-api and waited for 30000ms before read timeout.',
      timestamp: dayjs().second(35).millisecond(5).toISOString(),
      service: 'payment-sapi',
      relatedLogIds: ['log-007', 'log-008'],
      relatedTimelineEventIds: ['tl-006'],
    },
    {
      id: 'ev-004',
      title: 'Client received HTTP 500 after timeout',
      description:
        'payment-sapi mapped the HUB timeout to PAYMENT_PROPOSE_TIMEOUT and returned HTTP 500.',
      timestamp: dayjs().second(35).millisecond(100).toISOString(),
      service: 'payment-sapi',
      relatedLogIds: ['log-009'],
      relatedTimelineEventIds: ['tl-007'],
    },
  ]

  if (similarTimeouts > 0) {
    evidence.push({
      id: 'ev-005',
      title: 'Expanded window shows repeated HUB timeouts',
      description: `${similarTimeouts} similar timeout errors were found for hub-payment-propose-api in the last hour.`,
      timestamp: dayjs().add(3, 'minute').toISOString(),
      service: 'payment-sapi',
      relatedLogIds: ['log-010'],
      relatedTimelineEventIds: ['tl-008'],
    })
  }

  return evidence
}

function createTimeline(similarTimeouts: number): TimelineEvent[] {
  const base = dayjs().second(3).millisecond(120)
  const events: TimelineEvent[] = [
    {
      id: 'tl-001',
      timestamp: base.toISOString(),
      service: 'payment-sapi',
      eventType: 'inbound_request',
      status: 'OK',
      message: 'Received request from gateway',
      relatedLogIds: ['log-001', 'log-002'],
    },
    {
      id: 'tl-002',
      timestamp: base.add(330, 'millisecond').toISOString(),
      service: 'payment-sapi',
      eventType: 'validation',
      status: 'OK',
      durationMs: 330,
      message: 'Validation passed',
      relatedLogIds: ['log-003'],
    },
    {
      id: 'tl-003',
      timestamp: base.add(890, 'millisecond').toISOString(),
      service: 'payment-sapi',
      eventType: 'downstream_request',
      status: 'OK',
      message: 'Calling payee-service',
      relatedLogIds: ['log-004'],
      downstreamCallId: 'call-payee',
    },
    {
      id: 'tl-004',
      timestamp: base.add(1120, 'millisecond').toISOString(),
      service: 'payee-service',
      eventType: 'downstream_response',
      status: 'OK',
      durationMs: 230,
      message: 'payee-service returned 200',
      relatedLogIds: ['log-005'],
      downstreamCallId: 'call-payee',
    },
    {
      id: 'tl-005',
      timestamp: base.add(1300, 'millisecond').toISOString(),
      service: 'payment-sapi',
      eventType: 'downstream_response',
      status: 'OK',
      durationMs: 180,
      message: 'limit-service returned 200',
      relatedLogIds: ['log-006'],
      downstreamCallId: 'call-limit',
    },
    {
      id: 'tl-006',
      timestamp: base.add(1880, 'millisecond').toISOString(),
      service: 'payment-sapi',
      eventType: 'downstream_request',
      status: 'TIMEOUT',
      durationMs: 30000,
      message: 'hub-payment-propose-api read timeout',
      relatedLogIds: ['log-007', 'log-008'],
      downstreamCallId: 'call-hub',
    },
    {
      id: 'tl-007',
      timestamp: base.add(31980, 'millisecond').toISOString(),
      service: 'payment-sapi',
      eventType: 'outbound_response',
      status: 'FAILED',
      message: 'Returned HTTP 500 to caller',
      relatedLogIds: ['log-009'],
    },
  ]

  if (similarTimeouts > 0) {
    events.push({
      id: 'tl-008',
      timestamp: base.add(3, 'minute').toISOString(),
      service: 'payment-sapi',
      eventType: 'similar_timeout_cluster',
      status: 'WARNING',
      message: `${similarTimeouts} similar HUB timeout errors found`,
      relatedLogIds: ['log-010'],
    })
  }

  return events
}

function createDownstreamCalls(): DownstreamCall[] {
  return [
    {
      id: 'call-payee',
      caller: 'payment-sapi',
      downstream: 'payee-service',
      operation: 'GET /payees/{id}',
      endpoint: '/payees/{id}',
      status: 'OK',
      latencyMs: 230,
      relatedLogIds: ['log-004', 'log-005'],
    },
    {
      id: 'call-limit',
      caller: 'payment-sapi',
      downstream: 'limit-service',
      operation: 'POST /limits/check',
      endpoint: '/limits/check',
      status: 'OK',
      latencyMs: 180,
      relatedLogIds: ['log-006'],
    },
    {
      id: 'call-hub',
      caller: 'payment-sapi',
      downstream: 'hub-payment-propose-api',
      operation: 'POST /payments/propose',
      endpoint: '/payments/propose',
      status: 'TIMEOUT',
      latencyMs: 30000,
      errorMessage: 'Read timeout',
      relatedLogIds: ['log-007', 'log-008'],
    },
  ]
}

function serviceStatus(serviceName: string): EventStatus {
  return serviceName === 'hub-payment-propose-api' ? 'TIMEOUT' : 'OK'
}

function createServiceGraph(): ServiceGraph {
  return {
    nodes: [
      {
        id: 'gateway',
        serviceName: 'gateway',
        platform: 'Edge',
        status: 'OK',
        logCount: 1,
        errorCount: 0,
      },
      {
        id: 'payment-sapi',
        serviceName: 'payment-sapi',
        platform: 'SHP_AWS',
        status: 'FAILED',
        logCount: 46,
        errorCount: 2,
      },
      {
        id: 'payee-service',
        serviceName: 'payee-service',
        platform: 'SHP_AWS',
        status: serviceStatus('payee-service'),
        logCount: 13,
        errorCount: 0,
      },
      {
        id: 'limit-service',
        serviceName: 'limit-service',
        platform: 'SHP_AWS',
        status: serviceStatus('limit-service'),
        logCount: 9,
        errorCount: 0,
      },
      {
        id: 'hub-payment-propose-api',
        serviceName: 'hub-payment-propose-api',
        platform: 'HUB',
        status: serviceStatus('hub-payment-propose-api'),
        logCount: 17,
        errorCount: 5,
      },
    ],
    edges: [
      {
        id: 'edge-gateway-payment',
        source: 'gateway',
        target: 'payment-sapi',
        label: 'request',
        operation: 'POST /payments/propose',
        status: 'OK',
        latencyMs: 40,
        evidenceType: 'CONFIRMED',
        relatedLogIds: ['log-001', 'log-002'],
      },
      {
        id: 'edge-payment-payee',
        source: 'payment-sapi',
        target: 'payee-service',
        label: '200 / 230ms',
        operation: 'GET /payees/{id}',
        status: 'OK',
        latencyMs: 230,
        evidenceType: 'CONFIRMED',
        relatedLogIds: ['log-004', 'log-005'],
        downstreamCallId: 'call-payee',
      },
      {
        id: 'edge-payment-limit',
        source: 'payment-sapi',
        target: 'limit-service',
        label: '200 / 180ms',
        operation: 'POST /limits/check',
        status: 'OK',
        latencyMs: 180,
        evidenceType: 'CONFIRMED',
        relatedLogIds: ['log-006'],
        downstreamCallId: 'call-limit',
      },
      {
        id: 'edge-payment-hub',
        source: 'payment-sapi',
        target: 'hub-payment-propose-api',
        label: 'Timeout / 30000ms',
        operation: 'POST /payments/propose',
        status: 'TIMEOUT',
        latencyMs: 30000,
        evidenceType: 'CONFIRMED',
        relatedLogIds: ['log-007', 'log-008'],
        downstreamCallId: 'call-hub',
      },
    ],
  }
}

function createSequence(): SequenceViewModel {
  return {
    participants: [
      { id: 'gateway', label: 'Gateway', serviceName: 'gateway' },
      { id: 'payment', label: 'Payment SAPI', serviceName: 'payment-sapi' },
      { id: 'payee', label: 'Payee API', serviceName: 'payee-service' },
      { id: 'limit', label: 'Limit API', serviceName: 'limit-service' },
      { id: 'hub', label: 'HUB API', serviceName: 'hub-payment-propose-api' },
    ],
    messages: [
      {
        id: 'seq-001',
        from: 'gateway',
        to: 'payment',
        label: 'request',
        timestamp: dayjs().second(3).millisecond(120).toISOString(),
        status: 'OK',
        relatedLogIds: ['log-001', 'log-002'],
      },
      {
        id: 'seq-002',
        from: 'payment',
        to: 'payee',
        label: 'get payee',
        timestamp: dayjs().second(4).millisecond(10).toISOString(),
        status: 'OK',
        durationMs: 230,
        relatedLogIds: ['log-004', 'log-005'],
        downstreamCallId: 'call-payee',
      },
      {
        id: 'seq-003',
        from: 'payment',
        to: 'limit',
        label: 'check limit',
        timestamp: dayjs().second(4).millisecond(420).toISOString(),
        status: 'OK',
        durationMs: 180,
        relatedLogIds: ['log-006'],
        downstreamCallId: 'call-limit',
      },
      {
        id: 'seq-004',
        from: 'payment',
        to: 'hub',
        label: 'propose payment',
        timestamp: dayjs().second(5).millisecond(0).toISOString(),
        status: 'TIMEOUT',
        durationMs: 30000,
        relatedLogIds: ['log-007', 'log-008'],
        downstreamCallId: 'call-hub',
      },
      {
        id: 'seq-005',
        from: 'payment',
        to: 'gateway',
        label: '500 PAYMENT_PROPOSE_TIMEOUT',
        timestamp: dayjs().second(35).millisecond(100).toISOString(),
        status: 'FAILED',
        relatedLogIds: ['log-009'],
      },
    ],
  }
}

function createQueries(input: InvestigationInput, similarTimeouts: number): SplQueryRecord[] {
  const timeRange = input.timeRange
  const correlationId = input.correlationId ?? 'abc-123'

  const queries: SplQueryRecord[] = [
    {
      id: 'spl-001',
      templateName: 'Find logs by correlation ID',
      reason: `User provided correlation ID ${correlationId}`,
      spl: `index=payment_${input.environment.toLowerCase()} correlationId="${correlationId}" earliest="${timeRange.from}" latest="${timeRange.to}" | sort _time`,
      timeRange,
      resultCount: similarTimeouts > 0 ? 148 : 86,
      executionDurationMs: 1200,
      status: 'SUCCESS',
      splunkUrl: 'https://splunk.local/mock/search?spl=spl-001',
    },
    {
      id: 'spl-002',
      templateName: 'Find downstream timeout evidence',
      reason: 'Failure hypothesis points to downstream timeout',
      spl: `index=payment_${input.environment.toLowerCase()} correlationId="${correlationId}" "hub-payment-propose-api" ("timeout" OR "Read timed out")`,
      timeRange,
      resultCount: similarTimeouts > 0 ? 18 : 5,
      executionDurationMs: 860,
      status: 'SUCCESS',
      splunkUrl: 'https://splunk.local/mock/search?spl=spl-002',
    },
  ]

  if (similarTimeouts > 0) {
    queries.push({
      id: 'spl-003',
      templateName: 'Find similar HUB timeouts',
      reason: 'Follow-up requested expanded search window',
      spl: `index=payment_${input.environment.toLowerCase()} service=payment-sapi downstream=hub-payment-propose-api status=TIMEOUT earliest="${timeRange.from}" latest="${timeRange.to}" | stats count by correlationId`,
      timeRange,
      resultCount: similarTimeouts,
      executionDurationMs: 1420,
      status: 'SUCCESS',
      splunkUrl: 'https://splunk.local/mock/search?spl=spl-003',
    })
  }

  return queries
}

function createSummary(similarTimeouts: number): DiagnosisSummary {
  return {
    status: 'FAILED',
    finalHttpStatus: 500,
    failurePoint: 'hub-payment-propose-api timeout',
    rootCauseHypothesis:
      similarTimeouts > 0
        ? 'HUB propose API appears degraded during the expanded window.'
        : 'payment-sapi waited 30 seconds for HUB propose response and then returned HTTP 500.',
    confidence: 'HIGH',
    logsFound: similarTimeouts > 0 ? 148 : 86,
    affectedServices: [
      'gateway',
      'payment-sapi',
      'payee-service',
      'limit-service',
      'hub-payment-propose-api',
    ],
    evidence: createEvidence(similarTimeouts),
    recommendedActions:
      similarTimeouts > 0
        ? [
            'Check HUB service health for the last 1 hour.',
            'Escalate to HUB support with 12 matching timeout correlations.',
            'Keep payment-sapi retry and timeout settings unchanged until HUB confirms recovery.',
          ]
        : [
            'Check HUB service health around the failure timestamp.',
            'Search similar HUB timeout errors in the last 30 minutes.',
            'Escalate to HUB support if the expanded search shows a spike.',
          ],
  }
}

function createResult(
  investigationId: string,
  input: InvestigationInput,
  runNumber: number,
  similarTimeouts: number,
): InvestigationResult {
  const runId = `run-${runNumber}`

  return {
    investigationId,
    runId,
    runNumber,
    context: {
      environment: input.environment,
      timeRange: input.timeRange,
      correlationId: input.correlationId,
      apiName: input.apiName,
      market: input.market,
      lastRunAt: dayjs().toISOString(),
    },
    summary: createSummary(similarTimeouts),
    timeline: createTimeline(similarTimeouts),
    serviceGraph: createServiceGraph(),
    sequence: createSequence(),
    downstreamCalls: createDownstreamCalls(),
    rawLogs: createRawLogsMock({
      correlationId: input.correlationId ?? 'abc-123',
      runNumber,
      similarTimeouts,
    }),
    queries: createQueries(input, similarTimeouts),
    suggestedFollowUps: createSuggestedFollowUps(),
  }
}

function createNoResultInvestigation(input: InvestigationInput): Investigation {
  const investigationId = createId('inv')
  const runId = 'run-1'
  const result: InvestigationResult = {
    investigationId,
    runId,
    runNumber: 1,
    context: {
      environment: input.environment,
      timeRange: input.timeRange,
      correlationId: input.correlationId,
      apiName: input.apiName,
      market: input.market,
      lastRunAt: dayjs().toISOString(),
    },
    summary: {
      status: 'NO_RESULT',
      confidence: 'UNKNOWN',
      logsFound: 0,
      affectedServices: [],
      evidence: [],
      recommendedActions: [
        'Expand the time range.',
        'Check the correlation ID value.',
        'Search by API name if no correlation ID is available.',
      ],
    },
    timeline: [],
    serviceGraph: { nodes: [], edges: [] },
    sequence: { participants: [], messages: [] },
    downstreamCalls: [],
    rawLogs: [],
    queries: createQueries(input, 0).map((query) => ({
      ...query,
      resultCount: 0,
    })),
    suggestedFollowUps: createSuggestedFollowUps(),
  }

  return {
    id: investigationId,
    activeRunId: runId,
    createdAt: dayjs().toISOString(),
    updatedAt: dayjs().toISOString(),
    input,
    runs: [
      {
        runId,
        runNumber: 1,
        title: 'Initial search',
        action: 'ANSWER_FROM_CURRENT_RESULT',
        summary: 'No matching logs found.',
        createdAt: dayjs().toISOString(),
      },
    ],
    activeResult: result,
    conversation: [
      createChatMessage(
        'ASSISTANT',
        'No matching logs were found for the selected input and time range.',
        'ANSWER_FROM_CURRENT_RESULT',
        runId,
      ),
    ],
  }
}

export function createMockInvestigation(
  request: StartInvestigationRequest = {
    rawText: mockUserInput,
    environment: 'SIT',
    timeRangeLabel: 'Last 30 min',
  },
): Investigation {
  const input = createInput(request)

  if (request.rawText.toLowerCase().includes('no-result')) {
    return createNoResultInvestigation(input)
  }

  const investigationId = createId('inv')
  const result = createResult(investigationId, input, 1, 0)
  const runId = result.runId

  return {
    id: investigationId,
    activeRunId: runId,
    createdAt: dayjs().toISOString(),
    updatedAt: dayjs().toISOString(),
    input,
    runs: [
      {
        runId,
        runNumber: 1,
        title: 'Initial search by correlation ID',
        action: 'ANSWER_FROM_CURRENT_RESULT',
        summary:
          'Found payment-sapi failure caused by hub-payment-propose-api timeout.',
        createdAt: dayjs().toISOString(),
      },
    ],
    activeResult: result,
    conversation: [
      createChatMessage(
        'ASSISTANT',
        'I found logs for the correlation and built an investigation timeline. The likely failure point is hub-payment-propose-api timeout.',
        'ANSWER_FROM_CURRENT_RESULT',
        runId,
      ),
    ],
  }
}

export function createRerunInvestigation(
  investigation: Investigation,
  userQuestion: string,
): Investigation {
  const runNumber = investigation.runs.length + 1
  const input: InvestigationInput = {
    ...investigation.input,
    timeRange: buildTimeRange('Last 1 hour'),
  }
  const activeResult = createResult(investigation.id, input, runNumber, 12)
  const runSummary = {
    runId: activeResult.runId,
    runNumber,
    title: 'Expanded to last 1 hour',
    userQuestion,
    action: 'RERUN_QUERY' as const,
    summary:
      'Found 12 similar HUB timeout errors for hub-payment-propose-api.',
    createdAt: dayjs().toISOString(),
  }

  return {
    ...investigation,
    activeRunId: activeResult.runId,
    updatedAt: dayjs().toISOString(),
    input,
    runs: [...investigation.runs, runSummary],
    activeResult,
  }
}

export function appendConversation(
  investigation: Investigation,
  messages: ChatMessage[],
): Investigation {
  return {
    ...investigation,
    updatedAt: dayjs().toISOString(),
    conversation: [...investigation.conversation, ...messages],
  }
}

export function mockMessage(
  role: ChatMessage['role'],
  content: string,
  actionType?: FollowUpActionType,
  relatedRunId?: string,
) {
  return createChatMessage(role, content, actionType, relatedRunId)
}
