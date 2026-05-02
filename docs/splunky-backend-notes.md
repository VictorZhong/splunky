# Splunky Backend Notes

## Current Scaffold

- Module: `splunky-service`
- Runtime: Java 17 + Spring Boot 3
- Package root: `com.wpb.spky`
- Storage: PostgreSQL for schema/session metadata/LLM credentials/audit metadata, in-memory for active investigation response payloads
- Database: Flyway-managed PostgreSQL
- Splunk client: Splunk Java SDK, session-scoped user credential, cached SDK service per Splunky session
- Investigation API: frontend-compatible response shape with real Splunk fetch plus AI/fallback summary

## Java 17

Java 17 is already installed locally:

```sh
/Users/victorzhong/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home
```

Use:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
```

The Maven compiler release is fixed to 17 in `splunky-service/pom.xml`.
The repo also includes `.java-version` with `17` for tools that honor it.

## Session Credentials

`SessionCredentialManager` holds Splunk username/password only in runtime memory.

Rules:

- Splunk password is accepted by `POST /api/sessions/splunk-login`.
- Splunk password is not returned by API responses.
- Splunk password is not persisted.
- The login response returns a session ID. Frontend requests should send it as `X-Splunky-Session-Id`.
- Sessions use an idle timeout. Activity slides expiry by `SPKY_SESSION_TTL_MINUTES`, default 30 minutes.
- Logout immediately marks the session ended and removes the cached Splunk SDK service.
- The backend still accepts legacy `X-SPKY-Session-Id` while contracts are being aligned.
- Session metadata is persisted to `spky_user_session`; reusable Splunk secrets are not persisted.

There is a temporary compatibility switch:

```yaml
splunky.session.accept-frontend-generated-sessions: true
```

This lets the current frontend mock's generated session ID call backend placeholder APIs before real login wiring is finished.

Real Splunk-backed investigation calls require a session created by Splunk login, because the backend needs the current user's Splunk password in runtime memory.

## Audit Events

`spky_audit_event` records key operations with `staff_id`, `user_id`, `session_id`, event type, timestamps, and non-sensitive metadata.

Current events:

- `SESSION_LOGIN`
- `SESSION_LOGOUT`
- `SESSION_EXPIRED`
- `INVESTIGATION_CREATED`
- `INVESTIGATION_RERUN`
- `FOLLOW_UP_RECEIVED`
- `SPLUNK_QUERY_EXECUTED`
- `SPLUNK_QUERY_FAILED`
- `SPLUNK_URL_IMPORTED`
- `SPLUNK_URL_IMPORT_FAILED`

Audit metadata must not include Splunk passwords or raw Splunk rows.

## LLM Provider

The LLM layer follows the `chat2pay` pattern:

- `LlmProvider` abstraction
- `LlmRouter` primary/fallback provider selection
- `CopilotPersonalLlmProvider`
- `RemoteApiLlmProvider`
- OpenAI-compatible chat completion payload mapping
- tool-call payload/response support

Current provider order:

```yaml
splunky.llm.primary-provider: COPILOT_PERSONAL
splunky.llm.fallback-provider: REMOTE_API
```

## Database

Local default:

```sh
DB_URL=jdbc:postgresql://localhost:5432/splunky
DB_USER=splunky
DB_PASSWORD=splunky
```

Flyway is enabled by default:

```yaml
spring.flyway.table: spky_flyway_schema_history
spring.flyway.locations: classpath:db/migration
```

Current migration:

```text
splunky-service/src/main/resources/db/migration/V1__init_spky_tables.sql
splunky-service/src/main/resources/db/migration/V2__seed_query_templates.sql
splunky-service/src/main/resources/db/migration/V3__audit_staff_session_fields.sql
```

`V3` adds `staff_id` to audit events and created/updated staff fields to core metadata tables.

## Splunk Integration

The first backend version supports two investigation entry points:

1. Free text plus selected time/timezone. The backend asks the configured LLM to produce bounded read-only SPL, then falls back to a safe heuristic search when no LLM credential is configured.
2. Splunk URL. The backend parses `q=` searches or `sid=` jobs from Splunk URLs and fetches results through the Splunk SDK using the current Splunky session credential.

Runtime configuration:

```sh
SPLUNK_SCHEME=https
SPLUNK_HOST=digital-splunk-search.hk.zzzz
SPLUNK_PORT=8089
SPLUNK_TRUST_ALL_SSL=true
SPLUNK_JOB_TTL_SECONDS=1800
SPLUNK_MAX_AUTH_RETRY=3
SPLUNK_RESULT_ROW_LIMIT=0
```

The SDK implementation mirrors the verified internal flow:

- TLSv1.2 setup with optional trust-all SSL for internal certificates.
- `Service.connect` and `service.login()` using current user's Splunk username/password.
- Service cache keyed by Splunky session ID.
- Blocking search jobs with earliest/latest time.
- 401 retry with re-login and backoff.
- Job TTL update.
- JSON result reading through `ResultsReaderJson`.

The response currently fills Summary, Raw Logs preview, and Query Inspector. Timeline, service graph, sequence view, downstream calls, and AI assistant workflows remain deferred.

## Credential Handling

`JpaLlmCredentialStore` is used by default.

Supported sources:

- `LLM_API_KEY` as Copilot bootstrap API key
- `COPILOT_SESSION_TOKEN` as temporary bootstrap session token
- `PUT /api/llm-credential` for runtime replacement

Secrets are stored as plaintext in `spky_llm_credential` (`encrypted_secret` column name is historical).
The refreshed Copilot session token is also stored in the same table.
The store returns status and fingerprint metadata via API, and never returns raw secret in status payload.

## Copilot Refresh

`CopilotPersonalLlmProvider` refreshes Copilot session tokens automatically:

1. Read configured API key/session token from `LlmCredentialStore`.
2. If the session token is fresh, use it.
3. If missing or expiring, call `COPILOT_TOKEN_URL`.
4. Cache the returned session token and expiry.
5. On chat completion 401, clear the session token and retry one refresh.

Default token URL:

```sh
https://api.github.com/copilot_internal/v2/token
```

Default Copilot base URL:

```sh
https://api.githubcopilot.com
```

## Proxy

Copilot traffic supports `LLM_PROXY_URL`:

```sh
LLM_PROXY_URL=http://username:password@proxy-host:80
```

Notes:

- Special characters in username/password must be percent-encoded.
- HTTP Basic proxy auth is configured through Apache HttpClient 5.
- 407 responses are mapped to actionable configuration errors.

## Current API Shape

The backend currently supports the frontend MVP shape under `/api/investigations`.

The older OpenAPI document in `docs/splunky-api-contract.yaml` has a richer response wrapper and different session header name. Aligning that contract with the frontend/backend implementation is a P0 TODO.
