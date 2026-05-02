# Splunky Database Design

## Purpose

This document describes the effective PostgreSQL design for the current Splunky MVP.

The main rule is unchanged:

- persist metadata that is useful for operation and later history
- do not persist Splunk passwords
- do not persist raw Splunk log payloads

## Platform

| Item | Value |
|---|---|
| Database | PostgreSQL 16 |
| Table prefix | `spky_` |
| Flyway history table | `spky_flyway_schema_history` |
| Schema owner | Flyway |
| JPA mode | `ddl-auto: validate` |

## What Is Persisted Today

- `spky_user_account`
- `spky_user_session`
- `spky_audit_event`
- `spky_query_template`
- `spky_llm_credential`

The investigation result object returned to the frontend is still held in memory while the summary-only flow is being stabilized.

## Data That Must Not Be Persisted

- Splunk password
- reusable Splunk login secret
- raw Splunk log rows

Raw log rows may be returned to the frontend as transient previews, but they are not durable DB records.

## LLM Credential Model

`spky_llm_credential` now follows the same simplified model used in `chat2pay`.

Design intent:

- one row per provider
- operators update `api_key` manually in SQL
- backend refreshes `session_token` automatically
- no encryption for the current internal MVP

Table shape:

| Column | Type | Notes |
|---|---|---|
| `provider` | `varchar(32)` | PK, `COPILOT_PERSONAL` or `REMOTE_API` |
| `api_key` | `text` | manually managed operator secret |
| `session_token` | `text` | short-lived provider token cached by backend |
| `session_token_expires_at` | `timestamptz` | token expiry |
| `metadata_json` | `jsonb` | provider-specific future metadata |
| `updated_at` | `timestamptz` | last backend/operator update |

Example operator SQL:

```sql
insert into spky_llm_credential (provider, api_key, updated_at)
values ('COPILOT_PERSONAL', 'ghp_xxx', now())
on conflict (provider) do update
set api_key = excluded.api_key,
    updated_at = now();
```

## Session Metadata Model

`spky_user_session` stores:

- session id
- linked user id
- Splunk username
- selected environment
- status
- start / expiry / last activity / end timestamps

It does not store the Splunk password.

## Flyway Strategy

Current migrations:

- `V1__init_spky_tables.sql`
- `V2__seed_query_templates.sql`
- `V3__audit_staff_session_fields.sql`
- `V4__simplify_llm_credential_storage.sql`

`V4` intentionally drops and recreates `spky_llm_credential`. This is acceptable because operator-managed API keys can be reinserted directly and the old per-user/per-credential-type model is no longer wanted.

This is the preferred approach here:

- keep migration history intact
- do not rewrite old version numbers
- use a fresh forward migration when the shape changes

## Query Templates

`spky_query_template` remains durable because approved SPL templates are useful independently of the summary-only UI scope.

## Audit Events

`spky_audit_event` stores non-sensitive operational metadata:

- `user_id`
- `session_id`
- `staff_id`
- `event_type`
- `target_type`
- `target_id`
- `summary`
- `metadata_json`
- `created_at`

Audit rows must not include Splunk passwords or raw log content.
