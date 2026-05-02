# Splunky Frontend MVP Notes

## Current Scope

The frontend MVP is now deliberately summary-only.

What ships:

- login screen
- environment selection
- free-text / Splunk URL investigation input
- summary workspace
- raw log preview
- executed SPL viewer
- detail drawer for evidence, logs, and SPL

What is deferred:

- timeline tab
- service graph
- sequence view
- downstream calls view
- AI assistant panel

## Login Flow

- first screen is `/login`
- users choose the Splunk environment before entering username/password
- login page shows the supported environment list on the left
- backend owns the real session; frontend stores only lightweight session metadata in `sessionStorage`

The UI must state clearly that:

- Splunk password is used only for the current backend session
- Splunk password is not persisted

## Investigation Flow

The query page keeps one primary input box for:

- Splunk URL
- correlation ID
- error response
- natural-language troubleshooting request

The backend is responsible for:

- input interpretation
- safe SPL generation
- Splunk querying
- AI summarization

The frontend is responsible for:

- collecting the input and time range
- showing progress/loading states
- rendering the returned summary/evidence/raw logs/SPL cleanly

## Workspace Layout

The current workspace should show:

1. result context bar
2. AI summary
3. evidence list
4. raw log preview sent to AI
5. executed SPL

This keeps the first release focused on one reliable path:

`Splunk logs -> AI summary -> readable UI`

## Session And Unauthorized Handling

- requests send `X-Splunky-Session-Id`
- backend `401` means the Splunk credential/session must be re-established
- frontend should route the user back to login with a clear explanation

## Configuration

- build-time defaults live in `.env.*`
- runtime overrides live in `public/config/splunky-config.js`
- `mockMode=on` uses MSW
- `mockMode=off` calls the configured backend API
