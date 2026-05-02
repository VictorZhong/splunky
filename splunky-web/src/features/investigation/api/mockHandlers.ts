import { delay, http, HttpResponse } from 'msw'
import type {
  FollowUpRequest,
  Investigation,
  StartInvestigationRequest,
} from '../types'
import { handleMockFollowUp } from '../../../mocks/followUpMock'
import { createMockInvestigation } from '../../../mocks/investigationMock'

const investigationStore = new Map<string, Investigation>()
const sessionStore = new Map<string, MockSession>()

type MockSession = {
  sessionId: string
  splunkUsername: string
  environment: string
  startedAt: string
  expiresAt: string
}

type MockLoginRequest = {
  splunkUsername: string
  splunkPassword: string
  environment: string
}

function createSessionId() {
  if (window.crypto.randomUUID) {
    return window.crypto.randomUUID()
  }
  return `mock-session-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function saveInvestigation(investigation: Investigation) {
  investigationStore.set(investigation.id, investigation)
  return investigation
}

function hasSession(request: Request) {
  const sessionId = request.headers.get('X-Splunky-Session-Id')
  return Boolean(sessionId && sessionStore.has(sessionId))
}

function unauthorized() {
  return HttpResponse.json(
    { message: 'Splunk credentials may be invalid. Please log in again.' },
    { status: 401 },
  )
}

export const investigationHandlers = [
  http.post('/api/sessions/splunk-login', async ({ request }) => {
    const body = (await request.json()) as MockLoginRequest
    await delay(250)

    if (
      !body?.splunkUsername?.trim() ||
      !body?.splunkPassword?.trim() ||
      !body?.environment?.trim()
    ) {
      return HttpResponse.json(
        { message: 'Splunk username, password, and environment are required.' },
        { status: 401 },
      )
    }

    const startedAt = new Date().toISOString()
    const expiresAt = new Date(Date.now() + 30 * 60 * 1000).toISOString()
    const session: MockSession = {
      sessionId: createSessionId(),
      splunkUsername: body.splunkUsername.trim(),
      environment: body.environment.trim().toUpperCase(),
      startedAt,
      expiresAt,
    }
    sessionStore.set(session.sessionId, session)

    return HttpResponse.json({
      sessionId: session.sessionId,
      user: {
        userId: '00000000-0000-4000-8000-000000000000',
        username: session.splunkUsername,
        displayName: session.splunkUsername,
        email: null,
        status: 'ACTIVE',
      },
      splunkUsername: session.splunkUsername,
      environment: session.environment,
      status: 'ACTIVE',
      startedAt: session.startedAt,
      expiresAt: session.expiresAt,
      lastActivityAt: session.startedAt,
    })
  }),

  http.get('/api/sessions/current', async ({ request }) => {
    const sessionId = request.headers.get('X-Splunky-Session-Id')
    if (!sessionId || !sessionStore.has(sessionId)) {
      return HttpResponse.json(
        { message: 'Missing or expired Splunky session.' },
        { status: 401 },
      )
    }

    const session = sessionStore.get(sessionId)!
    return HttpResponse.json({
      sessionId: session.sessionId,
      user: {
        userId: '00000000-0000-4000-8000-000000000000',
        username: session.splunkUsername,
        displayName: session.splunkUsername,
        email: null,
        status: 'ACTIVE',
      },
      splunkUsername: session.splunkUsername,
      environment: session.environment,
      status: 'ACTIVE',
      startedAt: session.startedAt,
      expiresAt: session.expiresAt,
      lastActivityAt: session.startedAt,
    })
  }),

  http.delete('/api/sessions/current', async ({ request }) => {
    const sessionId = request.headers.get('X-Splunky-Session-Id')
    if (sessionId) {
      sessionStore.delete(sessionId)
    }
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('/api/investigations', async ({ request }) => {
    if (!hasSession(request)) {
      return unauthorized()
    }

    const body = (await request.json()) as StartInvestigationRequest
    await delay(900)

    if (body.rawText.toLowerCase().includes('auth-error')) {
      return unauthorized()
    }

    if (body.rawText.toLowerCase().includes('mock-error')) {
      return HttpResponse.json(
        {
          message:
            'Splunky could not complete the mock query. Please retry or start a new investigation.',
        },
        { status: 500 },
      )
    }

    return HttpResponse.json(saveInvestigation(createMockInvestigation(body)))
  }),

  http.get('/api/investigations/:id', async ({ params, request }) => {
    if (!hasSession(request)) {
      return unauthorized()
    }

    await delay(250)
    const investigation = investigationStore.get(String(params.id))

    if (!investigation) {
      return HttpResponse.json(
        { message: 'Investigation not found in mock store.' },
        { status: 404 },
      )
    }

    return HttpResponse.json(investigation)
  }),

  http.post('/api/investigations/:id/follow-ups', async ({ params, request }) => {
    if (!hasSession(request)) {
      return unauthorized()
    }

    const investigation = investigationStore.get(String(params.id))
    const body = (await request.json()) as FollowUpRequest
    await delay(750)

    if (body.prompt.toLowerCase().includes('auth-error')) {
      return unauthorized()
    }

    if (!investigation) {
      return HttpResponse.json(
        { message: 'Investigation not found in mock store.' },
        { status: 404 },
      )
    }

    const response = handleMockFollowUp(investigation, body.prompt)
    saveInvestigation(response.investigation)

    return HttpResponse.json(response)
  }),
]
