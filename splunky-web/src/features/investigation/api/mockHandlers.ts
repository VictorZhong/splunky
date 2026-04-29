import { delay, http, HttpResponse } from 'msw'
import type {
  FollowUpRequest,
  Investigation,
  StartInvestigationRequest,
} from '../types'
import { handleMockFollowUp } from '../../../mocks/followUpMock'
import { createMockInvestigation } from '../../../mocks/investigationMock'

const investigationStore = new Map<string, Investigation>()

function saveInvestigation(investigation: Investigation) {
  investigationStore.set(investigation.id, investigation)
  return investigation
}

function hasSession(request: Request) {
  return Boolean(request.headers.get('X-Splunky-Session-Id'))
}

function unauthorized() {
  return HttpResponse.json(
    { message: 'Splunk credentials may be invalid. Please log in again.' },
    { status: 401 },
  )
}

export const investigationHandlers = [
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
