CREATE SCHEMA IF NOT EXISTS web;
CREATE SCHEMA IF NOT EXISTS platform;

CREATE TABLE web.users
(
    user_id    BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE web.roles ();
CREATE TABLE web.user_roles ();

CREATE TABLE platform.topic_metadata
(
    topic_id       BIGSERIAL PRIMARY KEY,
    topic_name     VARCHAR(255) NOT NULL UNIQUE,
    display_name   VARCHAR(255),
    description    TEXT,
    schema_json    JSONB,
    time_field     VARCHAR(100),
    message_format VARCHAR(50) DEFAULT 'json',
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE platform.topic_permission
(
    permission_id   BIGSERIAL PRIMARY KEY,
    topic_id        BIGINT      NOT NULL REFERENCES platform.topic_metadata (topic_id),
    user_id         BIGINT      NOT NULL REFERENCES web.users (user_id),
    permission_type VARCHAR(50) NOT NULL,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE platform.pipeline
(
    pipeline_id              BIGSERIAL PRIMARY KEY,
    owner_user_id            BIGINT       NOT NULL REFERENCES web.users (user_id),
    pipeline_name            VARCHAR(255) NOT NULL,
    pipeline_type            VARCHAR(50)  NOT NULL,
    status                   VARCHAR(50)  NOT NULL,
    natural_language_request TEXT,
    pipeline_plan_json       JSONB,
    generated_sql            TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE platform.pipeline_sink_config
(
    sink_config_id  BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT       NOT NULL REFERENCES platform.pipeline (pipeline_id),
    sink_type       VARCHAR(50)  NOT NULL DEFAULT 'POSTGRESQL',
    sink_table_name VARCHAR(255) NOT NULL,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE platform.pipeline_deployment
(
    deployment_id   BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT      NOT NULL REFERENCES platform.pipeline (pipeline_id),
    deployment_type VARCHAR(50) NOT NULL,
    flink_job_id    VARCHAR(255),
    flink_jar_id    VARCHAR(500),
    status          VARCHAR(50) NOT NULL,
    started_at      TIMESTAMP,
    stopped_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    last_checked_at TIMESTAMP,
    error_message   TEXT,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);

CREATE TABLE platform.pipeline_artifact
(
    artifact_id        BIGSERIAL PRIMARY KEY,
    pipeline_id        BIGINT       NOT NULL REFERENCES platform.pipeline (pipeline_id),
    artifact_type      VARCHAR(50)  NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path   TEXT         NOT NULL,
    flink_jar_id       VARCHAR(500),
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);
CREATE TABLE platform.custom_job_config
(
    config_id     BIGSERIAL PRIMARY KEY,
    pipeline_id   BIGINT       NOT NULL REFERENCES platform.pipeline (pipeline_id),
    entry_class   VARCHAR(500) NOT NULL,
    input_topics  JSONB        NOT NULL,
    output_topics JSONB,
    parallelism   INT       DEFAULT 1,
    program_args  JSONB,
    created_by               VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP DEFAULT now() NOT NULL,
    updated_by               VARCHAR(255) NOT NULL,
    updated_at               TIMESTAMP DEFAULT now() NOT NULL
);