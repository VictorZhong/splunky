import { create } from 'zustand'

const storageKey = 'splunky.session'

export interface AuthSession {
  sessionId: string
  username: string
  environment: string
  createdAt: string
}

interface SessionState {
  session: AuthSession | null
  setSession: (session: AuthSession) => void
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

export const useSessionStore = create<SessionState>((set) => ({
  session: readSession(),
  setSession: (session) => {
    window.sessionStorage.setItem(storageKey, JSON.stringify(session))
    set({ session })
  },
  logout: () => {
    window.sessionStorage.removeItem(storageKey)
    set({ session: null })
  },
}))

export function getCurrentSession() {
  return useSessionStore.getState().session
}
