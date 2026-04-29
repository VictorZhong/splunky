import dayjs from 'dayjs'
export function formatTimestamp(timestamp: string) {
  return dayjs(timestamp).format('HH:mm:ss.SSS')
}

export function formatDateTime(timestamp: string) {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDuration(durationMs?: number) {
  if (durationMs === undefined) {
    return '-'
  }

  if (durationMs >= 1000) {
    return `${(durationMs / 1000).toFixed(durationMs % 1000 === 0 ? 0 : 1)}s`
  }

  return `${durationMs}ms`
}

export function compactNumber(value: number) {
  return new Intl.NumberFormat('en-US', { notation: 'compact' }).format(value)
}
