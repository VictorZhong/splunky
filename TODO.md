# Splunky TODO

## P0

- Validate the end-to-end Splunk SDK flow inside the intranet with real staff credentials for each supported environment.
- Validate Copilot credential rotation by manually `insert`/`update`-ing `spky_llm_credential` in PostgreSQL.
- Tighten free-text query planning with approved SPL templates for common flows: correlation ID, API error response, service name, and customer/reference IDs.
- Add backend integration tests for Splunk 401 re-login, Splunk URL `sid=` import, client error mapping, and Copilot token refresh retry behavior.
- Add row-limit, truncation, and timeout safeguards for large Splunk result sets before broader internal rollout.

## P1

- Persist investigation/run/query/chat metadata once the summary-only workflow is considered stable.
- Add stored history APIs after DB-backed investigation persistence exists.
- Expand the summary prompt/output package for stronger structured evidence extraction.
- Add local dev profiles and proxy defaults for running `splunky-web` against `splunky-service`.

## P2

- Reintroduce deferred workspace views after the summary-only MVP stabilizes: timeline, service graph, sequence, downstream calls.
- Reintroduce AI assistant and follow-up UX after the summary experience is reliable enough to justify the extra surface area.
- Add query-template management APIs and operator tooling if template volume grows beyond SQL/Flyway maintenance.
