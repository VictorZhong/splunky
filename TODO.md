# Splunky TODO

## P0

- Align `docs/splunky-api-contract.yaml`, frontend API calls, and backend controller DTOs into one contract.
- Wire frontend login to `POST /api/sessions/splunk-login` so Splunk password reaches backend session memory instead of only generating a browser-side session ID.
- Validate the Splunk SDK integration inside the intranet with real staff credentials and the internal search head.
- Tighten free-text query planning with approved SPL templates for common flows: correlation ID, API error response, app/service name, customer ID.
- Decide frontend behavior for Splunk URL time override versus selected UI time range.
- Wire frontend summary view to backend `POST /api/investigations` response and require `X-Splunky-Session-Id`.

## P1

- Add DB-backed persistence for investigation/run/chat metadata beyond the current placeholder in-memory active result store.
- Add encrypted DB-backed user-scoped LLM credential storage once login/profile ownership is finalized. Current implementation stores app-level encrypted Copilot credentials in `spky_llm_credential`.
- Persist investigation metadata, run metadata, chat messages, query metadata, and derived structured evidence.
- Keep raw Splunk logs transient; never persist full raw payloads.
- Add integration tests for Splunk 401 re-login, Splunk URL `sid=` import, Splunk client error mapping, and LLM token refresh retry behavior.
- Add pagination/streaming safeguards for large Splunk result sets.

## P2

- Expand LLM prompt package for structured evidence extraction beyond summary.
- Add run history APIs once DB persistence exists.
- Add query-template management API from the OpenAPI contract.
- Add audit events for LLM credential changes and future export/share actions.
- Add local dev profile and frontend proxy configuration for running `splunky-web` against `splunky-service`.
- Implement deferred views after summary stabilizes: timeline, service graph, sequence view, downstream calls, and AI assistant follow-ups.
