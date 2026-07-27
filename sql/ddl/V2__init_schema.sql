-- =========================================================
-- StreamCell Initial Schema
-- PostgreSQL
-- =========================================================

CREATE SCHEMA IF NOT EXISTS web;
CREATE SCHEMA IF NOT EXISTS platform;

-- =========================================================
-- web schema
-- 사용자 / 권한 / 화면 영역
-- =========================================================

CREATE TABLE IF NOT EXISTS web.users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS web.roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS web.user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES web.users(user_id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES web.roles(role_id) ON DELETE CASCADE,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);

-- =========================================================
-- platform schema
-- Kafka Topic / Pipeline / Flink Job / Artifact 영역
-- =========================================================

CREATE TABLE IF NOT EXISTS platform.topic_metadata (
    topic_id BIGSERIAL PRIMARY KEY,
    topic_name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    description TEXT,
    schema_json JSONB,
    time_field VARCHAR(100),
    message_format VARCHAR(50) NOT NULL DEFAULT 'json',
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform.topic_permission (
    permission_id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES platform.topic_metadata(topic_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES web.users(user_id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_topic_permission UNIQUE (topic_id, user_id, permission_type)
);

CREATE TABLE IF NOT EXISTS platform.pipeline (
    pipeline_id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES web.users(user_id),
    pipeline_name VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    pipeline_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    natural_language_request TEXT,
    pipeline_plan_json JSONB,
    generated_sql TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform.pipeline_sink_config (
    sink_config_id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES platform.pipeline(pipeline_id) ON DELETE CASCADE,
    sink_type VARCHAR(50) NOT NULL DEFAULT 'POSTGRESQL',
    sink_table_name VARCHAR(255) NOT NULL,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform.pipeline_deployment (
    deployment_id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES platform.pipeline(pipeline_id) ON DELETE CASCADE,
    deployment_type VARCHAR(50) NOT NULL,
    flink_job_id VARCHAR(255),
    flink_jar_id VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    stopped_at TIMESTAMP,
    finished_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    error_message TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform.pipeline_artifact (
    artifact_id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES platform.pipeline(pipeline_id) ON DELETE CASCADE,
    artifact_type VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path TEXT NOT NULL,
    flink_jar_id VARCHAR(500),
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform.custom_job_config (
    config_id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES platform.pipeline(pipeline_id) ON DELETE CASCADE,
    entry_class VARCHAR(500) NOT NULL,
    input_topics JSONB NOT NULL,
    output_topics JSONB,
    parallelism INT NOT NULL DEFAULT 1,
    program_args JSONB,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_custom_job_parallelism CHECK (parallelism >= 1)
);

CREATE TABLE IF NOT EXISTS platform.ai_failure_analysis (
    analysis_id BIGSERIAL PRIMARY KEY,
    pipeline_id BIGINT NOT NULL REFERENCES platform.pipeline(pipeline_id) ON DELETE CASCADE,
    deployment_id BIGINT REFERENCES platform.pipeline_deployment(deployment_id) ON DELETE SET NULL,
    raw_error TEXT,
    ai_summary TEXT,
    ai_recommendation TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

-- =========================================================
-- indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_topic_permission_user_id
    ON platform.topic_permission(user_id);

CREATE INDEX IF NOT EXISTS idx_topic_permission_topic_id
    ON platform.topic_permission(topic_id);

CREATE INDEX IF NOT EXISTS idx_pipeline_owner_user_id
    ON platform.pipeline(owner_user_id);

CREATE INDEX IF NOT EXISTS idx_pipeline_status
    ON platform.pipeline(status);

CREATE INDEX IF NOT EXISTS idx_pipeline_type
    ON platform.pipeline(pipeline_type);

CREATE INDEX IF NOT EXISTS idx_pipeline_deployment_pipeline_id
    ON platform.pipeline_deployment(pipeline_id);

CREATE INDEX IF NOT EXISTS idx_pipeline_deployment_flink_job_id
    ON platform.pipeline_deployment(flink_job_id);

CREATE INDEX IF NOT EXISTS idx_pipeline_artifact_pipeline_id
    ON platform.pipeline_artifact(pipeline_id);

CREATE INDEX IF NOT EXISTS idx_custom_job_config_pipeline_id
    ON platform.custom_job_config(pipeline_id);

-- =========================================================
-- seed data
-- MVP 테스트용 사용자/역할
-- password는 임시값입니다. 실제 구현 시 BCrypt 등으로 교체하세요.
-- =========================================================

INSERT INTO web.roles (role_name, description, created_by, created_at, updated_by, updated_at)
VALUES
    ('ADMIN', 'StreamCell administrator', 'ADMIN', now(), 'ADMIN', now()),
    ('ANALYST', 'AI SQL pipeline user', 'ADMIN', now(), 'ADMIN', now()),
    ('ENGINEER', 'Custom Flink job user', 'ADMIN', now(), 'ADMIN', now())
    ON CONFLICT (role_name) DO NOTHING;

INSERT INTO web.users (email, password, name, status, created_by, created_at, updated_by, updated_at)
VALUES
    ('admin@streamcell.local', 'admin1234', 'Admin User', 'ACTIVE', 'ADMIN', now(), 'ADMIN', now()),
    ('analyst@streamcell.local', 'analyst1234', 'Analyst User', 'ACTIVE', 'ADMIN', now(), 'ADMIN', now()),
    ('engineer@streamcell.local', 'engineer1234', 'Engineer User', 'ACTIVE', 'ADMIN', now(), 'ADMIN', now())
    ON CONFLICT (email) DO NOTHING;

INSERT INTO web.user_roles (user_id, role_id, created_by, created_at, updated_by, updated_at)
SELECT u.user_id, r.role_id, 'ADMIN', now(), 'ADMIN', now()
FROM web.users u
         JOIN web.roles r ON r.role_name = 'ADMIN'
WHERE u.email = 'admin@streamcell.local'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO web.user_roles (user_id, role_id, created_by, created_at, updated_by, updated_at)
SELECT u.user_id, r.role_id, 'ADMIN', now(), 'ADMIN', now()
FROM web.users u
         JOIN web.roles r ON r.role_name = 'ANALYST'
WHERE u.email = 'analyst@streamcell.local'
    ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO web.user_roles (user_id, role_id, created_by, created_at, updated_by, updated_at)
SELECT u.user_id, r.role_id, 'ADMIN', now(), 'ADMIN', now()
FROM web.users u
         JOIN web.roles r ON r.role_name = 'ENGINEER'
WHERE u.email = 'engineer@streamcell.local'
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- =========================================================
-- sample topic metadata
-- Kafka에 orders topic을 만든 뒤 sync API로 넣어도 되지만,
-- 초기 테스트 편의를 위해 seed로도 등록합니다.
-- =========================================================

INSERT INTO platform.topic_metadata (
    topic_name,
    display_name,
    description,
    schema_json,
    time_field,
    message_format, created_by, created_at, updated_by, updated_at
)
VALUES (
           'orders',
           '주문 이벤트',
           '주문 발생 시 생성되는 샘플 이벤트',
           '{
             "order_id": "STRING",
             "user_id": "STRING",
             "product_id": "STRING",
             "payment_amount": "DECIMAL(10,2)",
             "event_time": "TIMESTAMP(3)"
           }'::jsonb,
           'event_time',
           'json',
        'ADMIN', now(), 'ADMIN', now()
       )
    ON CONFLICT (topic_name) DO NOTHING;

-- admin: orders 전체 권한
INSERT INTO platform.topic_permission (topic_id, user_id, permission_type, created_by, created_at, updated_by, updated_at)
SELECT t.topic_id, u.user_id, p.permission_type, 'ADMIN', now(), 'ADMIN', now()
FROM platform.topic_metadata t
         JOIN web.users u ON u.email = 'admin@streamcell.local'
         CROSS JOIN (
    VALUES ('VIEW'), ('QUERY'), ('DEPLOY'), ('ADMIN')
) AS p(permission_type)
WHERE t.topic_name = 'orders'
    ON CONFLICT (topic_id, user_id, permission_type) DO NOTHING;

-- analyst: VIEW, QUERY
INSERT INTO platform.topic_permission (topic_id, user_id, permission_type, created_by, created_at, updated_by, updated_at)
SELECT t.topic_id, u.user_id, p.permission_type, 'ADMIN', now(), 'ADMIN', now()
FROM platform.topic_metadata t
         JOIN web.users u ON u.email = 'analyst@streamcell.local'
         CROSS JOIN (
    VALUES ('VIEW'), ('QUERY')
) AS p(permission_type)
WHERE t.topic_name = 'orders'
    ON CONFLICT (topic_id, user_id, permission_type) DO NOTHING;

-- engineer: VIEW, QUERY, DEPLOY
INSERT INTO platform.topic_permission (topic_id, user_id, permission_type, created_by, created_at, updated_by, updated_at)
SELECT t.topic_id, u.user_id, p.permission_type, 'ADMIN', now(), 'ADMIN', now()
FROM platform.topic_metadata t
         JOIN web.users u ON u.email = 'engineer@streamcell.local'
         CROSS JOIN (
    VALUES ('VIEW'), ('QUERY'), ('DEPLOY')
) AS p(permission_type)
WHERE t.topic_name = 'orders'
    ON CONFLICT (topic_id, user_id, permission_type) DO NOTHING;