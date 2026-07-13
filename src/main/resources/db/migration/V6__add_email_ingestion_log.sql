-- V6__add_email_ingestion_log.sql
-- Email ingestion deduplication table.
-- Idempotency key = SHA-256 of normalized email content (or Message-ID header).

CREATE TABLE email_ingestion_log (
                                     id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     idempotency_key  VARCHAR(64) NOT NULL UNIQUE,
                                     application_id   UUID REFERENCES applications (id) ON DELETE SET NULL,
                                     classification   VARCHAR(20),   -- REJECTION, INTERVIEW, OTHER
                                     processed_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_email_ingestion_key ON email_ingestion_log (idempotency_key);