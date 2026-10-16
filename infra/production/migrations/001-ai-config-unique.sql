-- Optional, reviewed migration for an EXISTING DB that fails check-schema.sql
-- specifically because the AI_SQL conflict key is missing. Back up first.
-- This script never deletes duplicate rows; duplicates abort the transaction.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM platform.ai_config
        GROUP BY pipeline_id, input_topic_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate AI configurations exist; review them before adding the unique key';
    END IF;
END $$;
ALTER TABLE platform.ai_config
    ADD CONSTRAINT uk_ai_config_pipeline_topic UNIQUE (pipeline_id, input_topic_id);
COMMIT;
