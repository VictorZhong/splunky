import { setupWorker } from 'msw/browser'
import { investigationHandlers } from '../features/investigation/api/mockHandlers'

export const worker = setupWorker(...investigationHandlers)
