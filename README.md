# Splunky

Splunky is a summary-first Splunk investigation workspace for internal API troubleshooting.

The current MVP is intentionally narrow:

- log in with `environment + username + password`
- run a Splunk-backed investigation from free text or a Splunk URL
- send the returned log preview to the configured LLM
- render one structured summary, raw log preview, and executed SPL

Timeline, graph, sequence, downstream-call, and AI-assistant UI flows are deferred for now.

## Repo Layout

- `splunky-service`: Java 17 + Spring Boot backend
- `splunky-web`: React + Vite frontend
- `docs/`: current notes, API contract, and DB design

## Backend

Java 17 is required:

```sh
cd splunky-service
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

The backend starts on `http://localhost:8080` and currently exposes:

- `GET /actuator/health`
- `POST /api/sessions/splunk-login`
- `GET /api/sessions/current`
- `DELETE /api/sessions/current`
- `POST /api/investigations`
- `GET /api/investigations/{id}`
- `POST /api/investigations/{id}/follow-ups`
- `GET /api/llm-credential/status`
- `GET /api/query-templates`

Flyway owns the schema and JPA only validates it:

- Flyway history table: `spky_flyway_schema_history`
- migrations: `splunky-service/src/main/resources/db/migration`
- JPA mode: `ddl-auto: validate`

## Frontend

```sh
cd splunky-web
npm install
npm run dev
npm run test -- --run
npm run build
```

The current workspace is summary-only:

- top context bar
- AI summary
- evidence list
- raw log preview
- executed SPL

## LLM Credential Handling

`spky_llm_credential` now follows the same operator-managed shape as `chat2pay`:

- one row per provider
- `api_key` is inserted or updated manually in PostgreSQL
- `session_token` and `session_token_expires_at` are refreshed by the backend
- no encryption is applied in this internal MVP

Current columns:

- `provider`
- `api_key`
- `session_token`
- `session_token_expires_at`
- `metadata_json`
- `updated_at`

Example operator SQL:

```sql
insert into spky_llm_credential (provider, api_key, updated_at)
values ('COPILOT_PERSONAL', 'ghp_xxx', now())
on conflict (provider) do update
set api_key = excluded.api_key,
    updated_at = now();
```

Optional one-time bootstrap from env vars is still supported:

```sh
export LLM_API_KEY=...
export COPILOT_SESSION_TOKEN=
export LLM_PROXY_URL=http://username:password@proxy-host:80
export LLM_MODEL=gpt-5.4
```

If the DB row is missing and `LLM_API_KEY` or `COPILOT_SESSION_TOKEN` is provided, the backend inserts the first row once on startup. After that, rotation should be done directly in SQL.

## Splunk Session Handling

- Splunk password is accepted by `POST /api/sessions/splunk-login`
- Splunk password is kept only in backend runtime memory
- Splunk password is never persisted in PostgreSQL
- frontend requests send `X-Splunky-Session-Id`

## Local PostgreSQL

```sh
docker run --name splunky-postgres \
  -e POSTGRES_USER=splunky \
  -e POSTGRES_PASSWORD=splunky \
  -e POSTGRES_DB=splunky \
  -p 5432:5432 \
  -d postgres:16
```

Defaults:

```sh
DB_URL=jdbc:postgresql://localhost:5432/splunky
DB_USER=splunky
DB_PASSWORD=splunky
```

## Notes

- `docs/splunky-backend-notes.md` describes the current backend behavior.
- `docs/splunky-frontend-mvp-notes.md` describes the current frontend scope.
- `docs/splunky-db-design.md` documents the effective DB model.
- `docs/splunky-api-contract.yaml` is aligned to the current controllers/DTOs.
