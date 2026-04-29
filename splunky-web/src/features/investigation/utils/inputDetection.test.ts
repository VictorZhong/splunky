import { describe, expect, it } from 'vitest'
import {
  buildTimeRange,
  detectInputType,
  extractCorrelationId,
} from './inputDetection'

describe('inputDetection', () => {
  it('detects correlation IDs from natural text', () => {
    expect(detectInputType('Find logs for correlation ID abc-123')).toBe(
      'CORRELATION_ID',
    )
    expect(extractCorrelationId('Find logs for correlation ID abc-123')).toBe(
      'abc-123',
    )
  })

  it('detects error response payloads', () => {
    expect(detectInputType('{"error":"PAYMENT_PROPOSE_TIMEOUT"}')).toBe(
      'ERROR_RESPONSE',
    )
  })

  it('builds a concrete time range contract', () => {
    const range = buildTimeRange('Last 1 hour')
    expect(range.label).toBe('Last 1 hour')
    expect(range.from).toEqual(expect.any(String))
    expect(range.to).toEqual(expect.any(String))
  })
})
