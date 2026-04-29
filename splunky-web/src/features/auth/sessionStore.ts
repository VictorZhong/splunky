import { create } from 'zustand'

const storageKey = 'splunky.session'

export interface AuthSession {
  sessionId: string
  username: string
  createdAt: string
}

interface SessionState {
  session: AuthSession | null
  login: (username: string) => AuthSession
  logout: () => void
}

function readSession(): AuthSession | null {
  const value = window.sessionStorage.getItem(storageKey)
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value) as AuthSession
  } catch {
    window.sessionStorage.removeItem(storageKey)
    return null
  }
}

function createSessionId() {
  if (window.crypto.randomUUID) {
    return window.crypto.randomUUID()
  }

  return `session-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export const useSessionStore = create<SessionState>((set) => ({
  session: readSession(),
  login: (username) => {
    const session: AuthSession = {
      sessionId: createSessionId(),
      username,
      createdAt: new Date().toISOString(),
    }
    window.sessionStorage.setItem(storageKey, JSON.stringify(session))
    set({ session })
    return session
  },
  logout: () => {
    window.sessionStorage.removeItem(storageKey)
    set({ session: null })
  },
}))

export function getCurrentSession() {
  return useSessionStore.getState().session
}
