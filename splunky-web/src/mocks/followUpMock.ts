import type {
  FollowUpResponse,
  Investigation,
  StartInvestigationRequest,
} from '../features/investigation/types'
import { classifyFollowUp } from '../features/investigation/utils/followUpClassification'
import {
  appendConversation,
  createMockInvestigation,
  createRerunInvestigation,
  mockMessage,
} from './investigationMock'

function answerFromCurrentResult(prompt: string) {
  const lower = prompt.toLowerCase()

  if (lower.includes('frontend')) {
    return 'Based on the current evidence, this does not look like a frontend issue. The request reached payment-sapi successfully, validation passed, and the failure occurred while waiting for hub-payment-propose-api.'
  }

  if (lower.includes('evidence')) {
    return 'The strongest evidence is the payment-sapi downstream request log, the read timeout log after 30000ms, and the final HTTP 500 response with PAYMENT_PROPOSE_TIMEOUT.'
  }

  return 'The request failed because payment-sapi timed out while calling hub-payment-propose-api. Upstream validation and the payee and limit downstream calls completed successfully before that timeout.'
}

function refinedAnswer(prompt: string) {
  if (prompt.toLowerCase().includes('hub team') || prompt.toLowerCase().includes('write')) {
    return 'Message to HUB team: payment-sapi saw repeated read timeouts from hub-payment-propose-api on POST /payments/propose. One confirmed correlation returned HTTP 500 after 30000ms. Please check HUB availability and latency for the same window.'
  }

  return 'Incident summary: payment proposal requests reached payment-sapi and passed validation. The failing path is the downstream HUB propose call, which timed out after 30 seconds and caused payment-sapi to return HTTP 500.'
}

export function handleMockFollowUp(
  investigation: Investigation,
  prompt: string,
): FollowUpResponse {
  const actionType = classifyFollowUp(prompt)
  const userMessage = mockMessage('USER', prompt, actionType, investigation.activeRunId)

  if (actionType === 'RERUN_QUERY') {
    const rerun = createRerunInvestigation(investigation, prompt)
    const assistantMessage = mockMessage(
      'ASSISTANT',
      'I expanded the time range to the last 1 hour and found 12 similar HUB timeout errors for hub-payment-propose-api. The current result has been updated.',
      actionType,
      rerun.activeRunId,
    )

    return {
      investigation: appendConversation(rerun, [userMessage, assistantMessage]),
      actionType,
      requeryStatus: {
        title: 'Re-running investigation',
        changes: [
          'Time range: Last 1 hour',
          'Added filter: downstream = hub-payment-propose-api',
          'Query type: similar timeout search',
        ],
        queryType: 'similar timeout search',
      },
    }
  }

  if (actionType === 'NEW_INVESTIGATION') {
    const request: StartInvestigationRequest = {
      rawText: prompt,
      environment: investigation.input.environment,
      timeRangeLabel: investigation.input.timeRange.label,
      market: investigation.input.market,
    }
    const next = createMockInvestigation(request)
    return {
      investigation: appendConversation(next, [
        mockMessage('USER', prompt, actionType, next.activeRunId),
      ]),
      actionType,
    }
  }

  const assistantMessage = mockMessage(
    'ASSISTANT',
    actionType === 'REFINE_ANALYSIS'
      ? refinedAnswer(prompt)
      : answerFromCurrentResult(prompt),
    actionType,
    investigation.activeRunId,
  )

  return {
    investigation: appendConversation(investigation, [userMessage, assistantMessage]),
    actionType,
  }
}
