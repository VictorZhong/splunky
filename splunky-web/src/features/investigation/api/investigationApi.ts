import type {
  FollowUpRequest,
  FollowUpResponse,
  Investigation,
  StartInvestigationRequest,
} from '../types'

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new Error(body?.message ?? `Request failed with ${response.status}`)
  }

  return response.json() as Promise<T>
}

export async function startInvestigation(
  request: StartInvestigationRequest,
): Promise<Investigation> {
  const response = await fetch('/api/investigations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  return parseResponse<Investigation>(response)
}

export async function getInvestigation(id: string): Promise<Investigation> {
  const response = await fetch(`/api/investigations/${id}`)
  return parseResponse<Investigation>(response)
}

export async function submitFollowUp(
  investigationId: string,
  request: FollowUpRequest,
): Promise<FollowUpResponse> {
  const response = await fetch(`/api/investigations/${investigationId}/follow-ups`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  return parseResponse<FollowUpResponse>(response)
}
