import type {
  FollowUpRequest,
  FollowUpResponse,
  Investigation,
  StartInvestigationRequest,
} from '../types'
import { getCurrentSession } from '../../auth/sessionStore'
import { unauthorizedEventName } from '../../auth/authEvents'
import { buildApiUrl } from '../../../app/config'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent(unauthorizedEventName))
    }
    throw new ApiError(
      body?.message ?? `Request failed with ${response.status}`,
      response.status,
    )
  }

  return response.json() as Promise<T>
}

function authHeaders() {
  const session = getCurrentSession()

  return {
    'Content-Type': 'application/json',
    ...(session ? { 'X-Splunky-Session-Id': session.sessionId } : {}),
  }
}

export async function startInvestigation(
  request: StartInvestigationRequest,
): Promise<Investigation> {
  const response = await fetch(buildApiUrl('/investigations'), {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(request),
  })

  return parseResponse<Investigation>(response)
}

export async function getInvestigation(id: string): Promise<Investigation> {
  const response = await fetch(buildApiUrl(`/investigations/${id}`), {
    headers: authHeaders(),
  })
  return parseResponse<Investigation>(response)
}

export async function submitFollowUp(
  investigationId: string,
  request: FollowUpRequest,
): Promise<FollowUpResponse> {
  const response = await fetch(
    buildApiUrl(`/investigations/${investigationId}/follow-ups`),
    {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(request),
    },
  )

  return parseResponse<FollowUpResponse>(response)
}
