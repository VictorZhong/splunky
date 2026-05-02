# Splunky

Frontend-only Splunky PoC built with React 19, TypeScript, Vite, Ant Design 6,
Tailwind CSS 4, TanStack Query, Zustand, React Flow, CodeMirror 6, Recharts,
Mermaid, MSW, Vitest, Testing Library, and Playwright.

## Development

```bash
npm install
npm run dev
```

The dev server uses MSW to mock login-protected `/api/investigations` and
follow-up requests by default. Login generates a session ID for API calls and
does not store the password.

## Environment Configuration

Build-time defaults live in:

- `.env.development`
- `.env.production`
- `.env.example`

Supported values:

```bash
VITE_MOCK_MODE=on|off|auto
VITE_API_BASE_URL=/api
VITE_APP_BASE_PATH=/
VITE_ROUTER_BASENAME=/
```

Runtime overrides can be set in `public/config/splunky-config.js`. This file is
loaded before the React app starts and is intended for PCF/static hosting:

```js
window.__SPLUNKY_CONFIG__ = {
  mockMode: 'off',
  apiBaseUrl: 'https://splunky-api.example.com/api',
  routerBasename: '/',
}
```

Keep this config non-secret because it is served to the browser.

Useful mock triggers:

- `no-result` returns an empty investigation result.
- `mock-error` returns a mock backend error.
- `auth-error` returns a mock 401 and opens the re-login prompt.
- Follow-ups such as `Expand to last 1 hour` create a new run and replace the active result.

## Verification

```bash
npm run lint
npm run test
npm run build
npm run test:e2e -- --project=chromium
```

## PCF Deployment

The app is configured for the Staticfile buildpack:

```bash
npm run pcf:prepare
cf push
```

`public/Staticfile` is copied into `dist/` during the Vite build and enables
`pushstate`, so direct refreshes such as `/login` and
`/investigations/:id` serve `index.html` instead of returning a PCF 404.

For PCF split deployment with a dedicated frontend domain (no extra path), keep:

```bash
VITE_APP_BASE_PATH=/
VITE_ROUTER_BASENAME=/
```

If the app is deployed under a path prefix such as `/splunky/`, set both:

```bash
VITE_APP_BASE_PATH=/splunky/
VITE_ROUTER_BASENAME=/splunky
```
