import { buildApiUrl } from '../../app/config'

export class SessionApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'SessionApiError'
    this.status = status
  }
}

type SplunkLoginRequest = {
  splunkUsername: string
  splunkPassword: string
  environment: string
}

type SessionResponse = {
  sessionId: string
  splunkUsername: string
  environment: string
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new SessionApiError(
      body?.message ?? `Request failed with ${response.status}`,
      response.status,
    )
  }

  return response.json() as Promise<T>
}

export async function loginSession(
  request: SplunkLoginRequest,
): Promise<SessionResponse> {
  const response = await fetch(buildApiUrl('/sessions/splunk-login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  return parseResponse<SessionResponse>(response)
}

export async function logoutSession(sessionId: string): Promise<void> {
  const response = await fetch(buildApiUrl('/sessions/current'), {
    method: 'DELETE',
    headers: {
      'X-Splunky-Session-Id': sessionId,
    },
  })

  if (response.status === 204) {
    return
  }

  await parseResponse<void>(response)
}
