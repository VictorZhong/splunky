import { isMockModeEnabled } from '../app/config'

export async function enableMocking() {
  if (!isMockModeEnabled()) {
    return
  }

  const { worker } = await import('./browser')

  return worker.start({
    onUnhandledRequest: 'bypass',
  })
}
