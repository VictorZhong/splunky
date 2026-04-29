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

export const investigationHandlers = [
  http.post('/api/investigations', async ({ request }) => {
    const body = (await request.json()) as StartInvestigationRequest
    await delay(900)

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

  http.get('/api/investigations/:id', async ({ params }) => {
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
    const investigation = investigationStore.get(String(params.id))
    const body = (await request.json()) as FollowUpRequest
    await delay(750)

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
