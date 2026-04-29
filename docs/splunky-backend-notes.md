# Splunky Backend Notes

## Current Scaffold

- Module: `splunky-service`
- Runtime: Java 17 + Spring Boot 3
- Package root: `com.wpb.spky`
- Storage: in-memory only for now
- Database: deferred
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

## Credential Handling

Because DB is deferred, `InMemoryLlmCredentialStore` is used for now.

Supported sources:

- `LLM_API_KEY` as Copilot bootstrap API key
- `COPILOT_SESSION_TOKEN` as temporary bootstrap session token
- `PUT /api/llm-credential` for runtime in-memory replacement

The store returns only a non-sensitive SHA-256 fingerprint prefix. It never returns the raw secret.

Future DB-backed replacement should keep the same `LlmCredentialStore` interface and move credential storage to `spky_llm_credential`, encrypted at rest.

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
