-- Simplify LLM credential storage to one row per provider, matching the
-- chat2pay operator-managed pattern. Existing rows are dropped intentionally:
-- operators will reinsert api_key values manually after this migration.

drop index if exists idx_spky_llm_credential_user_provider;
drop index if exists idx_spky_llm_credential_status;

drop table if exists spky_llm_credential;

create table if not exists spky_llm_credential (
    provider varchar(32) primary key,
    api_key text,
    session_token text,
    session_token_expires_at timestamptz,
    metadata_json jsonb,
    updated_at timestamptz not null default now(),
    constraint spky_ck_credential_provider
        check (provider in ('COPILOT_PERSONAL', 'REMOTE_API'))
);
