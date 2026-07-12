-- V2__add_tags_reminders_events_tables.sql
-- Adds supporting tables for tagging, reminders, Kafka idempotency,
-- email ingestion, and public analytics sharing.

-- ───────────────────────────────────────────────────────────────
-- application_tags
-- ───────────────────────────────────────────────────────────────

CREATE TABLE application_tags (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                  tag VARCHAR(100) NOT NULL,
                                  source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                                      CHECK (source IN ('MANUAL', 'AI')),
                                  created_at TIMESTAMP NOT NULL DEFAULT now(),
                                  UNIQUE(application_id, tag)
);

CREATE INDEX idx_tags_application_id ON application_tags(application_id);
CREATE INDEX idx_tags_tag ON application_tags(tag);

-- ───────────────────────────────────────────────────────────────
-- reminders
-- ───────────────────────────────────────────────────────────────

CREATE TABLE reminders (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                           remind_at TIMESTAMP NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING','SENT','FAILED','CANCELLED')),
                           attempt_count INT NOT NULL DEFAULT 0,
                           created_at TIMESTAMP NOT NULL DEFAULT now(),
                           updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_application_id
    ON reminders(application_id);

CREATE INDEX idx_reminders_remind_at
    ON reminders(remind_at);

CREATE INDEX idx_reminders_status
    ON reminders(status);

-- ───────────────────────────────────────────────────────────────
-- processed_events
-- ───────────────────────────────────────────────────────────────

CREATE TABLE processed_events (
                                  consumer_group VARCHAR(100) NOT NULL,
                                  event_id UUID NOT NULL,
                                  processed_at TIMESTAMP NOT NULL,
                                  PRIMARY KEY (consumer_group, event_id)
);

-- ───────────────────────────────────────────────────────────────
-- email_ingestion_log
-- ───────────────────────────────────────────────────────────────

CREATE TABLE email_ingestion_log (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     idempotency_key VARCHAR(255) NOT NULL UNIQUE,
                                     application_id UUID REFERENCES applications(id) ON DELETE SET NULL,
                                     classification VARCHAR(50),
                                     processed_at TIMESTAMP NOT NULL
);

-- ───────────────────────────────────────────────────────────────
-- share_tokens
-- ───────────────────────────────────────────────────────────────

CREATE TABLE share_tokens (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              token VARCHAR(255) NOT NULL UNIQUE,
                              expires_at TIMESTAMP NOT NULL,
                              revoked BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_share_tokens_token
    ON share_tokens(token);