ALTER TABLE spky_audit_event
    ADD COLUMN staff_id VARCHAR(128);

CREATE INDEX idx_spky_audit_event_staff_created
    ON spky_audit_event(staff_id, created_at DESC);

ALTER TABLE spky_user_account
    ADD COLUMN created_by_staff_id VARCHAR(128),
    ADD COLUMN updated_by_staff_id VARCHAR(128);

ALTER TABLE spky_investigation
    ADD COLUMN created_by_staff_id VARCHAR(128),
    ADD COLUMN updated_by_staff_id VARCHAR(128);

ALTER TABLE spky_investigation_run
    ADD COLUMN created_by_staff_id VARCHAR(128);

ALTER TABLE spky_chat_message
    ADD COLUMN created_by_staff_id VARCHAR(128);

ALTER TABLE spky_query_execution
    ADD COLUMN created_by_staff_id VARCHAR(128),
    ADD COLUMN updated_by_staff_id VARCHAR(128);

ALTER TABLE spky_feedback
    ADD COLUMN created_by_staff_id VARCHAR(128);
