import dayjs from 'dayjs'
import type { InvestigationInputType, TimeRange } from '../types'

const uuidLikePattern =
  /\b([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}|[a-z]{2,8}-\d{2,8}|abc-123|def-456)\b/i

export function detectInputType(rawText: string): InvestigationInputType {
  const value = rawText.trim()
  const lower = value.toLowerCase()

  if (value.startsWith('{') || value.startsWith('[')) {
    return 'ERROR_RESPONSE'
  }

  if (lower.includes('correlation') || uuidLikePattern.test(value)) {
    return 'CORRELATION_ID'
  }

  if (/\b(api|sapi|service|endpoint)\b/i.test(value)) {
    return 'API_NAME_OR_FIELD'
  }

  return 'NATURAL_LANGUAGE'
}

export function extractCorrelationId(rawText: string): string | undefined {
  const explicit = rawText.match(/correlation(?:Id| ID| id)?[:=\s]+([a-z0-9-]+)/i)
  if (explicit?.[1]) {
    return explicit[1]
  }

  return rawText.match(uuidLikePattern)?.[1]
}

export function inferApiName(rawText: string): string | undefined {
  const match = rawText.match(/\b([a-z][a-z0-9-]+(?:sapi|api|service))\b/i)
  return match?.[1]?.toLowerCase()
}

export function buildTimeRange(label: string): TimeRange {
  const now = dayjs()
  const minutes = label.includes('15')
    ? 15
    : label.includes('1 hour')
      ? 60
      : label.includes('4 hours')
        ? 240
        : 30

  return {
    label,
    from: now.subtract(minutes, 'minute').toISOString(),
    to: now.toISOString(),
  }
}
