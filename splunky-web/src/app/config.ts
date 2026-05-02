type MockMode = 'auto' | 'on' | 'off'

type RuntimeConfig = {
  mockMode?: MockMode
  apiBaseUrl?: string
  routerBasename?: string
}

declare global {
  interface Window {
    __SPLUNKY_CONFIG__?: RuntimeConfig
  }
}

function normalizeMockMode(value: unknown): MockMode {
  return value === 'on' || value === 'off' || value === 'auto' ? value : 'auto'
}

function normalizeApiBaseUrl(value: unknown) {
  if (typeof value !== 'string') {
    return ''
  }

  return value.trim().replace(/\/+$/, '')
}

function normalizeRouterBasename(value: unknown) {
  if (typeof value !== 'string' || value.trim() === '' || value === './') {
    return '/'
  }

  const withLeadingSlash = value.startsWith('/') ? value : `/${value}`
  const withoutTrailingSlash = withLeadingSlash.replace(/\/+$/, '')

  return withoutTrailingSlash || '/'
}

const runtimeConfig = window.__SPLUNKY_CONFIG__ ?? {}

export const appConfig = {
  mockMode: normalizeMockMode(
    runtimeConfig.mockMode ?? import.meta.env.VITE_MOCK_MODE,
  ),
  apiBaseUrl: normalizeApiBaseUrl(
    runtimeConfig.apiBaseUrl ?? import.meta.env.VITE_API_BASE_URL,
  ),
  routerBasename: normalizeRouterBasename(
    runtimeConfig.routerBasename ??
      import.meta.env.VITE_ROUTER_BASENAME ??
      import.meta.env.BASE_URL,
  ),
}

export function isMockModeEnabled() {
  if (appConfig.mockMode === 'on') {
    return true
  }

  if (appConfig.mockMode === 'off') {
    return false
  }

  return import.meta.env.DEV
}

export function buildApiUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const apiBaseUrl = isMockModeEnabled() ? '/api' : appConfig.apiBaseUrl

  if (apiBaseUrl === '') {
    return normalizedPath
  }

  return `${apiBaseUrl}${normalizedPath}`
}

export function buildAppUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  if (appConfig.routerBasename === '/') {
    return normalizedPath
  }

  return `${appConfig.routerBasename}${normalizedPath}`
}
