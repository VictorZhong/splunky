# Splunky

Splunky is an AI-assisted investigation workspace for test-environment API troubleshooting.

## Backend

The backend scaffold lives in `splunky-service`.

This machine already has Java 17 installed at:

```sh
/Users/victorzhong/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home
```

Use Java 17 explicitly when building or running the backend, because the default shell/Maven runtime may point at a newer JDK:

```sh
cd splunky-service
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn spring-boot:run
```

The repo also includes `.java-version` with `17` for tools that honor it.

The service starts on `http://localhost:8080` and exposes:

- `GET /actuator/health`
- `POST /api/sessions/splunk-login`
- `GET /api/sessions/current`
- `DELETE /api/sessions/current`
- `POST /api/investigations`
- `GET /api/investigations/{id}`
- `POST /api/investigations/{id}/follow-ups`
- `GET /api/llm-credential/status`
- `PUT /api/llm-credential`

LLM configuration uses environment variables compatible with the `chat2pay` approach:

```sh
export LLM_API_KEY=...
export LLM_PROXY_URL=http://username:password@proxy-host:80
export LLM_MODEL=gpt-5.4
```

See `docs/splunky-backend-notes.md` and `TODO.md` for current backend scope and next steps.

## Local PostgreSQL

For local development, a dedicated PostgreSQL container can be started with:

```sh
docker run --name splunky-postgres \
  -e POSTGRES_USER=splunky \
  -e POSTGRES_PASSWORD=splunky \
  -e POSTGRES_DB=splunky \
  -p 5432:5432 \
  -d postgres:16
```

The backend defaults to:

```sh
DB_URL=jdbc:postgresql://localhost:5432/splunky
DB_USER=splunky
DB_PASSWORD=splunky
```
