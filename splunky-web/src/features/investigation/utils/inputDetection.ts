import dayjs from 'dayjs'
import type { InvestigationInputType, TimeRange, TimezoneOption } from '../types'

const uuidLikePattern =
  /\b([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}|[a-z]{2,8}-\d{2,8}|abc-123|def-456)\b/i

export function detectInputType(rawText: string): InvestigationInputType {
  return detectInputTypes(rawText)[0] ?? 'NATURAL_LANGUAGE'
}

export function detectInputTypes(rawText: string): InvestigationInputType[] {
  const value = rawText.trim()
  const lower = value.toLowerCase()
  const types = new Set<InvestigationInputType>()

  if (isSplunkUrl(value)) {
    types.add('SPLUNK_URL')
  }

  if (value.startsWith('{') || value.startsWith('[')) {
    types.add('ERROR_RESPONSE')
  }

  if (lower.includes('correlation') || uuidLikePattern.test(value)) {
    types.add('CORRELATION_ID')
  }

  if (/\b(api|sapi|service|endpoint)\b/i.test(value)) {
    types.add('API_NAME_OR_FIELD')
  }

  if (
    types.size === 0 ||
    lower.includes('why') ||
    lower.includes('find') ||
    lower.includes('check') ||
    lower.includes('what')
  ) {
    types.add('NATURAL_LANGUAGE')
  }

  return Array.from(types)
}

export function isSplunkUrl(rawText: string) {
  return /^https?:\/\/\S*\/(?:app|en-US\/app|splunkd)\//i.test(rawText.trim())
}

export function extractSplunkUrl(rawText: string) {
  return rawText.match(/https?:\/\/\S+/i)?.[0]
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

export const defaultTimezone: TimezoneOption = {
  label: 'HKT',
  offset: '+08:00',
}

export const timezoneOptions: TimezoneOption[] = [
  defaultTimezone,
  { label: 'UTC', offset: '+00:00' },
  { label: 'SGT', offset: '+08:00' },
  { label: 'JST', offset: '+09:00' },
  { label: 'BST', offset: '+01:00' },
  { label: 'EST', offset: '-05:00' },
]

export function buildTimeRange(
  label: string,
  timezone: TimezoneOption = defaultTimezone,
  customRange?: [dayjs.Dayjs, dayjs.Dayjs],
): TimeRange {
  if (label === 'From Splunk URL') {
    const now = dayjs()
    return {
      label,
      from: now.subtract(30, 'minute').toISOString(),
      to: now.toISOString(),
      timezone,
    }
  }

  if (label === 'Custom' && customRange) {
    return {
      label: 'Custom',
      from: customRange[0].toISOString(),
      to: customRange[1].toISOString(),
      timezone,
    }
  }

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
    timezone,
  }
}
