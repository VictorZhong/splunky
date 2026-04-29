import type { FollowUpActionType } from '../types'

export function classifyFollowUp(prompt: string): FollowUpActionType {
  const lower = prompt.toLowerCase()

  if (
    /\b(check|investigate|search)\b.*\b(correlation id|correlation|def-\d+|[a-z]{2,8}-\d{2,8})\b/.test(
      lower,
    ) ||
    lower.includes('now investigate') ||
    lower.includes('another error')
  ) {
    return 'NEW_INVESTIGATION'
  }

  if (
    lower.includes('expand') ||
    lower.includes('last 1 hour') ||
    lower.includes('similar') ||
    lower.includes('failed downstream') ||
    lower.includes('5xx spike') ||
    lower.includes('spike today') ||
    lower.includes('rerun')
  ) {
    return 'RERUN_QUERY'
  }

  if (
    lower.includes('incident') ||
    lower.includes('write') ||
    lower.includes('summarize') ||
    lower.includes('qa') ||
    lower.includes('only show evidence')
  ) {
    return 'REFINE_ANALYSIS'
  }

  return 'ANSWER_FROM_CURRENT_RESULT'
}
