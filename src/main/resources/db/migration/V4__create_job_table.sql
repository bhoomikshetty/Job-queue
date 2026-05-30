CREATE SCHEMA IF NOT EXISTS jobs;
DROP TABLE jobs.jobs;
CREATE TABLE jobs.jobs (
id                  serial8         PRIMARY KEY,
locked_by_worker_id VARCHAR(255),
name                VARCHAR(255)    NOT NULL,
type                VARCHAR(50)     NOT NULL,
status              VARCHAR(20)     NOT NULL DEFAULT 'pending',
args_json_string    json,
max_retries         INT             NOT NULL DEFAULT 0,
retries             INT             NOT NULL DEFAULT 0,
error_message       TEXT,
created_at          timestamptz     NOT NULL,
last_retried_at     timestamptz,
last_error_at       timestamptz,
next_retry_at       timestamptz,
scheduled_at        timestamptz,
locked_at           timestamptz
);

-- without this, every worker poll is a full table scan as jobs accumulate
CREATE INDEX idx_jobs_status_scheduled_at ON jobs.jobs (status, scheduled_at);