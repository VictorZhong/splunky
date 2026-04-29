import dayjs from 'dayjs'
import type { RawLogEntry } from '../features/investigation/types'

type LogsOptions = {
  correlationId: string
  runNumber: number
  similarTimeouts?: number
}

export function createRawLogsMock({
  correlationId,
  runNumber,
  similarTimeouts = 0,
}: LogsOptions): RawLogEntry[] {
  const base = dayjs().second(3).millisecond(120)
  const splunkUrl = 'https://splunk.local/mock/search'

  const logs: RawLogEntry[] = [
    {
      id: 'log-001',
      timestamp: base.toISOString(),
      level: 'INFO',
      service: 'gateway',
      eventType: 'inbound_request',
      message: 'Gateway accepted request and forwarded to payment-sapi',
      splunkUrl,
      fields: {
        correlationId,
        method: 'POST',
        path: '/payments/propose',
        status: 200,
        runNumber,
      },
    },
    {
      id: 'log-002',
      timestamp: base.add(40, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payment-sapi',
      eventType: 'inbound_request',
      message: 'Received propose payment request from gateway',
      splunkUrl,
      fields: {
        correlationId,
        market: 'HK',
        channel: 'WEB',
        requestId: 'req-payment-8821',
      },
    },
    {
      id: 'log-003',
      timestamp: base.add(330, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payment-sapi',
      eventType: 'validation',
      message: 'Request validation passed',
      splunkUrl,
      fields: {
        correlationId,
        validator: 'payment-propose-schema',
        durationMs: 330,
      },
    },
    {
      id: 'log-004',
      timestamp: base.add(890, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payment-sapi',
      eventType: 'downstream_request',
      message: 'Calling payee-service for payee validation',
      splunkUrl,
      fields: {
        correlationId,
        downstream: 'payee-service',
        endpoint: 'GET /payees/{id}',
      },
    },
    {
      id: 'log-005',
      timestamp: base.add(1120, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payee-service',
      eventType: 'downstream_response',
      message: 'Payee validation returned 200',
      splunkUrl,
      fields: {
        correlationId,
        status: 200,
        durationMs: 230,
      },
    },
    {
      id: 'log-006',
      timestamp: base.add(1300, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payment-sapi',
      eventType: 'downstream_response',
      message: 'Limit check returned 200',
      splunkUrl,
      fields: {
        correlationId,
        downstream: 'limit-service',
        endpoint: 'POST /limits/check',
        status: 200,
        durationMs: 180,
      },
    },
    {
      id: 'log-007',
      timestamp: base.add(1880, 'millisecond').toISOString(),
      level: 'INFO',
      service: 'payment-sapi',
      eventType: 'downstream_request',
      message: 'Calling hub-payment-propose-api',
      splunkUrl,
      fields: {
        correlationId,
        downstream: 'hub-payment-propose-api',
        endpoint: 'POST /payments/propose',
        timeoutMs: 30000,
      },
    },
    {
      id: 'log-008',
      timestamp: base.add(31885, 'millisecond').toISOString(),
      level: 'ERROR',
      service: 'payment-sapi',
      eventType: 'downstream_response',
      message: 'Read timeout from hub-payment-propose-api after 30000ms',
      splunkUrl,
      fields: {
        correlationId,
        downstream: 'hub-payment-propose-api',
        status: 'TIMEOUT',
        durationMs: 30000,
        exception: 'java.net.SocketTimeoutException: Read timed out',
      },
    },
    {
      id: 'log-009',
      timestamp: base.add(31980, 'millisecond').toISOString(),
      level: 'ERROR',
      service: 'payment-sapi',
      eventType: 'outbound_response',
      message: 'Returning HTTP 500 because HUB propose call timed out',
      splunkUrl,
      fields: {
        correlationId,
        status: 500,
        errorCode: 'PAYMENT_PROPOSE_TIMEOUT',
      },
    },
  ]

  if (similarTimeouts > 0) {
    logs.push({
      id: 'log-010',
      timestamp: base.add(3, 'minute').toISOString(),
      level: 'WARN',
      service: 'payment-sapi',
      eventType: 'similar_timeout_cluster',
      message: `${similarTimeouts} similar HUB timeout errors found in expanded window`,
      splunkUrl,
      fields: {
        correlationId,
        downstream: 'hub-payment-propose-api',
        similarTimeouts,
        window: 'Last 1 hour',
      },
    })
  }

  return logs
}
