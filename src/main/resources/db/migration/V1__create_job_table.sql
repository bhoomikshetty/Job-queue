CREATE SCHEMA IF NOT EXISTS jobs;

CREATE TABLE jobs.jobs (
id                  BIGSERIAL       PRIMARY KEY,
name                VARCHAR(255)    NOT NULL,
type                VARCHAR(50)     NOT NULL,
status              VARCHAR(20)     NOT NULL DEFAULT 'pending',
args_json_string    TEXT,
max_retries         INT             NOT NULL DEFAULT 0,
retries             INT             NOT NULL DEFAULT 0,
error_message       TEXT,
created_at          TEXT,
last_retried_at     TEXT
);
