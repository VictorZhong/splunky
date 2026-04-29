# Splunky TODO

## P0

- Align `docs/splunky-api-contract.yaml`, frontend API calls, and backend controller DTOs into one contract.
- Wire frontend login to `POST /api/sessions/splunk-login` so Splunk password reaches backend session memory instead of only generating a browser-side session ID.
- Implement `SplunkClient` for `POST /services/search/jobs`, job polling, result fetch, pagination, limits, and deep links.
- Add approved SPL template registry and query planner, starting with correlation ID and error-response flows.
- Replace investigation placeholder responses with real intent parsing, query planning, Splunk execution, normalization, and evidence building.

## P1

- Add DB-backed persistence for investigation/run/chat metadata beyond the current placeholder in-memory active result store.
- Add encrypted DB-backed user-scoped LLM credential storage once login/profile ownership is finalized. Current implementation stores app-level encrypted Copilot credentials in `spky_llm_credential`.
- Persist investigation metadata, run metadata, chat messages, query metadata, and derived structured evidence.
- Keep raw Splunk logs transient; never persist full raw payloads.
- Add integration tests for session 401, Splunk client error mapping, and LLM token refresh retry behavior.

## P2

- Add LLM prompt package for evidence summarization and follow-up classification.
- Add run history APIs once DB persistence exists.
- Add query-template management API from the OpenAPI contract.
- Add audit events for session login/logout, query execution, LLM credential changes, and follow-up reruns.
- Add local dev profile and frontend proxy configuration for running `splunky-web` against `splunky-service`.
