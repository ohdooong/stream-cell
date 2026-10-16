-- Read-only deployment gate. Run with: psql -v ON_ERROR_STOP=1 ... -f check-schema.sql
\set ON_ERROR_STOP on
DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('web', 'users', 'login_id'),
            ('web', 'users', 'password'),
            ('web', 'roles', 'role_name'),
            ('web', 'user_roles', 'role_id'),
            ('platform', 'topic_metadata', 'schema_json'),
            ('platform', 'topic_permission', 'permission_type'),
            ('platform', 'pipeline', 'description'),
            ('platform', 'pipeline_sink_config', 'sink_table_name'),
            ('platform', 'pipeline_deployment', 'error_exception_name'),
            ('platform', 'pipeline_deployment', 'error_timestamp'),
            ('platform', 'pipeline_artifact', 'stored_file_path'),
            ('platform', 'custom_job_config', 'input_topics'),
            ('platform', 'ai_config', 'input_topic_id'),
            ('platform', 'ai_failure_analysis', 'ai_summary')
        ) AS required(schema_name, table_name, column_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns c
            WHERE c.table_schema = item.schema_name
              AND c.table_name = item.table_name
              AND c.column_name = item.column_name
        ) THEN
            RAISE EXCEPTION 'Missing required schema element: %.%.%', item.schema_name, item.table_name, item.column_name;
        END IF;
    END LOOP;

    -- AI_SQL registration uses ON CONFLICT (pipeline_id, input_topic_id).
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class t ON t.oid = i.indrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'platform' AND t.relname = 'ai_config'
          AND i.indisunique AND i.indisvalid AND i.indimmediate
          AND i.indpred IS NULL AND i.indexprs IS NULL
          AND i.indnkeyatts = 2
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname)
              FROM unnest(i.indkey::smallint[]) WITH ORDINALITY AS k(attnum, ordinal)
              JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
              WHERE k.ordinal <= i.indnkeyatts
          ) = ARRAY['input_topic_id', 'pipeline_id']::text[]
    ) THEN
        RAISE EXCEPTION 'platform.ai_config requires UNIQUE (pipeline_id, input_topic_id); review duplicates before migration';
    END IF;
    RAISE NOTICE 'Required StreamCell schema checks passed';
END $$;
