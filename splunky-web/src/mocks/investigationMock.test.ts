import { describe, expect, it } from 'vitest'
import { createMockInvestigation } from './investigationMock'

describe('createMockInvestigation', () => {
  it('creates the default HUB timeout scenario', () => {
    const investigation = createMockInvestigation({
      rawText: 'correlation id abc-123',
      environment: 'SIT',
      timeRangeLabel: 'Last 30 min',
      apiName: 'payment-sapi',
      market: 'HK',
    })

    expect(investigation.activeResult.summary.status).toBe('FAILED')
    expect(investigation.activeResult.summary.failurePoint).toContain(
      'hub-payment-propose-api',
    )
    expect(investigation.activeResult.downstreamCalls).toContainEqual(
      expect.objectContaining({
        downstream: 'hub-payment-propose-api',
        status: 'TIMEOUT',
      }),
    )
  })

  it('creates a no-result state for the no-result trigger', () => {
    const investigation = createMockInvestigation({
      rawText: 'no-result abc-123',
      environment: 'SIT',
      timeRangeLabel: 'Last 30 min',
    })

    expect(investigation.activeResult.summary.status).toBe('NO_RESULT')
    expect(investigation.activeResult.rawLogs).toHaveLength(0)
  })
})
