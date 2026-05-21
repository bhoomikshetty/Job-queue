CREATE SCHEMA IF NOT EXISTS jobs;
DROP TABLE jobs.jobs;
CREATE TABLE jobs.jobs (
id                  serial8         PRIMARY KEY,
name                VARCHAR(255)    NOT NULL,
type                VARCHAR(50)     NOT NULL,
status              VARCHAR(20)     NOT NULL DEFAULT 'pending',
args_json_string    json,
max_retries         INT             NOT NULL DEFAULT 0,
retries             INT             NOT NULL DEFAULT 0,
error_message       TEXT,
created_at          timestamptz     NOT NULL,
last_retried_at     timestamptz
);