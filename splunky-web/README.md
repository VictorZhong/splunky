# Splunky Web

Frontend-only Splunky PoC built with React 19, TypeScript, Vite, Ant Design 6,
Tailwind CSS 4, TanStack Query, Zustand, React Flow, CodeMirror 6, Recharts,
Mermaid, MSW, Vitest, Testing Library, and Playwright.

## Development

```bash
npm install
npm run dev
```

The dev server uses MSW to mock `/api/investigations` and follow-up requests.

Useful mock triggers:

- `no-result` returns an empty investigation result.
- `mock-error` returns a mock backend error.
- Follow-ups such as `Expand to last 1 hour` create a new run and replace the active result.

## Verification

```bash
npm run lint
npm run test
npm run build
npm run test:e2e -- --project=chromium
```
