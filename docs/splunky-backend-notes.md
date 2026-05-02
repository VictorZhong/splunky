# Splunky Backend Notes

## Current Shape

- module: `splunky-service`
- runtime: Java 17 + Spring Boot 3
- package root: `com.wpb.spky`
- persistence: PostgreSQL for session metadata, audit events, query templates, and LLM credentials
- runtime-only state: active investigation payloads and Splunk passwords
- schema owner: Flyway
- JPA mode: `ddl-auto: validate`

## What The Backend Does Today

The current backend path is:

1. accept Splunk login with `environment + username + password`
2. keep the Splunk password only in memory for the active backend session
3. plan a bounded Splunk search from free text or import one from a Splunk URL
4. execute the search through the Splunk Java SDK
5. send a preview of the returned rows to the configured LLM
6. return a summary-first response containing:
   - diagnosis summary
   - evidence items
   - raw log preview
   - executed SPL

Timeline, service graph, sequence, downstream-call, and assistant UI flows are not part of the current frontend MVP.

## Session Handling

`SessionCredentialManager` keeps Splunk username/password only in runtime memory.

Rules:

- Splunk password is accepted by `POST /api/sessions/splunk-login`
- Splunk password is never returned by the API
- Splunk password is never persisted in PostgreSQL
- frontend requests send `X-Splunky-Session-Id`
- legacy `X-SPKY-Session-Id` is still accepted for compatibility
- session metadata is persisted to `spky_user_session`
- idle expiry defaults to 30 minutes through `SPKY_SESSION_TTL_MINUTES`

## Flyway And Schema Ownership

Flyway is enabled by default:

```yaml
spring:
  flyway:
    table: spky_flyway_schema_history
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
```

Current migrations:

- `V1__init_spky_tables.sql`
- `V2__seed_query_templates.sql`
- `V3__audit_staff_session_fields.sql`
- `V4__simplify_llm_credential_storage.sql`

`V4` intentionally drops and recreates `spky_llm_credential` to match the simpler `chat2pay` operator-managed model.

## LLM Credential Handling

`spky_llm_credential` now matches the `chat2pay` pattern:

- one row per provider
- operators manually insert/update `api_key`
- backend automatically refreshes and stores `session_token`
- no encryption is used in the current internal MVP

Effective columns:

- `provider`
- `api_key`
- `session_token`
- `session_token_expires_at`
- `metadata_json`
- `updated_at`

`GET /api/llm-credential/status` is retained for operational checks. There is no runtime API for replacing the API key anymore; SQL is the intended control plane.

Optional bootstrap behavior still exists:

- if `LLM_API_KEY` or `COPILOT_SESSION_TOKEN` is provided
- and no DB row exists yet
- the backend inserts the first row once on startup

After that, credential rotation should happen directly in PostgreSQL.

## Copilot Provider Flow

`CopilotPersonalLlmProvider` works like this:

1. load provider row from `spky_llm_credential`
2. use the cached `session_token` if still fresh
3. if missing or expiring, exchange `api_key` for a fresh Copilot token
4. persist the new `session_token` and expiry
5. if chat completion returns `401`, clear the cached session token and retry one refresh

Proxy support remains on `LLM_PROXY_URL`.

## Splunk Integration

Supported investigation entry points:

- free text plus time range/timezone
- Splunk URL with `q=` or `sid=`

The SDK implementation currently supports:

- environment-based Splunk endpoint routing
- optional trust-all SSL for internal certs
- `Service.connect` + `service.login()` using the current user's Splunk credential
- session-keyed service reuse
- job import by `sid`
- blocking result reads through `ResultsReaderJson`
- auth retry and re-login on `401`

## Persistence Scope

Persisted today:

- user/session metadata
- audit events
- query templates
- LLM credentials

Not persisted today:

- raw Splunk logs
- active investigation result payloads
- summary/history/chat records

Those investigation result objects are still held in memory while the summary-only workflow is being stabilized.
