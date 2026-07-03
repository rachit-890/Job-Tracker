-- V2__add_tags_reminders_events_tables.sql
-- Adds all remaining tables. Schema is finalized here; entities and
-- business logic are introduced in their respective phases:
--   application_tags   → Phase 2 (manual tags) + Phase 4 (AI auto-tagging)
--   reminders          → Phase 5 (reminder scheduling + delivery)
--   processed_events   → Phase 3 (Kafka consumer idempotency)
--   analytics_snapshot → Phase 3 (analytics consumer)
--   resume_embeddings  → Phase 4 (Gemini embeddings + caching)
--   email_ingestion_log→ Phase 7 (idempotent email parsing)
--   share_tokens       → Phase 7 (public read-only analytics link)

-- ─── application_tags ────────────────────────────────────────────────────────

CREATE TABLE application_tags (
                                  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                  tag            VARCHAR(100) NOT NULL,
                                  source         VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                                      CHECK (source IN ('MANUAL', 'AI')),
                                  created_at     TIMESTAMP NOT NULL DEFAULT now(),
                                  UNIQUE (application_id, tag)   -- no duplicate tags per application
);

CREATE INDEX idx_tags_application_id ON application_tags(application_id);
CREATE INDEX idx_tags_tag            ON application_tags(tag);

-- ─── reminders ───────────────────────────────────────────────────────────────

CREATE TABLE reminders (
                           id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                           remind_at      TIMESTAMP NOT NULL,
                           status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
                           attempt_count  INT NOT NULL DEFAULT 0,
                           created_at     TIMESTAMP NOT NULL DEFAULT now(),
                           updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_application_id ON reminders(application_id);
CREATE INDEX idx_reminders_remind_at      ON reminders(remind_at);
CREATE INDEX idx_reminders_status         ON reminders(status);

-- ─── processed_events ────────────────────────────────────────────────────────
-- Kafka consumer idempotency table. Each consumer group records which
-- event IDs it has already processed so duplicate deliveries are ignored.

CREATE TABLE processed_events (
                                  consumer_group VARCHAR(100) NOT NULL,
                                  event_id       UUID NOT NULL,
                                  processed_at   TIMESTAMP NOT NULL,
                                  PRIMARY KEY (consumer_group, event_id)
);

-- ─── analytics_snapshot ──────────────────────────────────────────────────────
-- Maintained by the analytics consumer (Phase 3).
-- Single row (or time-series rows) kept up-to-date by Kafka consumers
-- so dashboard reads never hit raw application rows.

CREATE TABLE analytics_snapshot (
                                    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    total_applications      INT NOT NULL DEFAULT 0,
                                    response_rate           NUMERIC(5,2),
                                    avg_time_to_response    NUMERIC(10,2),
                                    status_breakdown_json   JSONB,
                                    computed_at             TIMESTAMP NOT NULL
);

-- ─── resume_embeddings ───────────────────────────────────────────────────────
-- Cache for Gemini embedding vectors, keyed by content hash (not the
-- version label, which can change without content changing).
-- embedding_vector stored as TEXT (JSON array) until pgvector is available.

CREATE TABLE resume_embeddings (
                                   resume_hash       VARCHAR(64)  PRIMARY KEY,  -- SHA-256 of resume content
                                   resume_version    VARCHAR(100),               -- human-readable label, informational only
                                   embedding_model   VARCHAR(100) NOT NULL,
                                   embedding_version VARCHAR(50)  NOT NULL,
                                   embedding_vector  TEXT         NOT NULL,      -- JSON array of floats
                                   generated_at      TIMESTAMP    NOT NULL
);

-- ─── email_ingestion_log ─────────────────────────────────────────────────────
-- Idempotency log for email-parsing endpoint (Phase 7).
-- idempotency_key = SHA-256 of normalized email content, or Message-ID header.

CREATE TABLE email_ingestion_log (
                                     id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
                                     application_id   UUID REFERENCES applications(id) ON DELETE SET NULL,
                                     classification   VARCHAR(50),  -- REJECTION, INTERVIEW, OTHER
                                     processed_at     TIMESTAMP NOT NULL
);

-- ─── share_tokens ────────────────────────────────────────────────────────────
-- Public, read-only, anonymized analytics links (Phase 7).
-- Tokens expire after 30 days and can be manually revoked.

CREATE TABLE share_tokens (
                              id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              token      VARCHAR(255) NOT NULL UNIQUE,
                              expires_at TIMESTAMP    NOT NULL,
                              revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_share_tokens_token ON share_tokens(token);