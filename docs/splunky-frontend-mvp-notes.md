# Splunky Frontend MVP Notes

## Environment And Market

- MVP has only one backend target: testing environment.
- Environment selector is removed from the UI.
- Market selector is deferred. Add it back when backend contracts need market-aware Splunk query filters.

## Login And Session

- The first screen is login.
- Users enter Splunk username and password so the backend can query Splunk on their behalf.
- The frontend must tell users credentials are used for Splunk queries and are not saved.
- After login, the frontend generates a session ID and sends it with API calls.
- The frontend mock stores only `sessionId`, `username`, and `createdAt` in `sessionStorage`; it does not store the password.

## Unauthorized Handling

- If a backend Splunk query finds invalid or expired credentials, backend should return `401`.
- Frontend should show a modal explaining credentials may be invalid and require the user to log in again.
- Current mock supports this by returning 401 when session headers are missing or when a request includes `auth-error`.

## Time Range

- Preset ranges remain available for fast searches.
- Custom time range should expose start datetime, end datetime, and timezone.
- Default timezone is `HKT (+08:00)`.
- If the user enters a Splunk URL, the frontend may mark time as `From Splunk URL`, while still allowing the user to override it with a preset or custom range.

## Query Input

- The query page should keep one primary text input for Splunk URL, error response, correlation ID, or natural-language question.
- Do not show API, environment, or market as separate user inputs in the MVP.
- Input signal detection is multi-value and internal. It is not a single-select UI.
- The backend/AI should infer SPL, involved APIs, gateways, mesh hops, time clues, and failure evidence from the input and logs.

## Result Context And Summary

- The result context bar should show the original user input in full with wrapping.
- Do not show `Test environment` or a single `API` field as investigation context.
- Summary should include a related API / traffic-hop list because one investigation may involve gateway, istio, SAPI, downstream APIs, and external systems.

## Navigation

- Query page does not show a new-search action in the header.
- Workspace page shows `New Search`.
- `New Search` opens `/` in a new browser tab so the current investigation is not discarded.
