# Splunky Backend Notes

## Current Scaffold

- Module: `splunky-service`
- Runtime: Java 17 + Spring Boot 3
- Package root: `com.wpb.spky`
- Storage: PostgreSQL for schema/session metadata/LLM credentials, in-memory for active placeholder investigation responses
- Database: Flyway-managed PostgreSQL
- Splunk client: deferred
- Investigation API: frontend-compatible placeholder responses

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
- Sessions expire after `SPKY_SESSION_TTL_MINUTES`, default 480 minutes.
- The backend accepts both `X-Splunky-Session-Id` and `X-SPKY-Session-Id`.
- Session metadata is persisted to `spky_user_session`; reusable Splunk secrets are not persisted.

There is a temporary compatibility switch:

```yaml
splunky.session.accept-frontend-generated-sessions: true
```

This lets the current frontend mock's generated session ID call backend placeholder APIs before real login wiring is finished.

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
```

## Credential Handling

`JdbcLlmCredentialStore` is used by default.

Supported sources:

- `LLM_API_KEY` as Copilot bootstrap API key
- `COPILOT_SESSION_TOKEN` as temporary bootstrap session token
- `PUT /api/llm-credential` for runtime replacement

Secrets are encrypted before storage in `spky_llm_credential`. The store returns only a non-sensitive fingerprint. It never returns the raw secret.

Set `SPKY_SECRET_KEY` to a base64-encoded 32-byte key before storing real credentials outside local development. Without it, the backend uses a local development key and logs a warning.

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
