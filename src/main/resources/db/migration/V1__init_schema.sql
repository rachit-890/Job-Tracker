-- V1__init_schema.sql
-- Phase 1 schema: users, applications, application_status_history.
-- Later phases will add tags, reminders, processed_events, analytics_snapshot,
-- resume_embeddings, email_ingestion_log, share_tokens.

CREATE TABLE users (
                       id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username      VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE applications (
                              id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              company         VARCHAR(255) NOT NULL,
                              role            VARCHAR(255) NOT NULL,
                              jd_text         TEXT,
                              resume_version  VARCHAR(100),
                              source_url      VARCHAR(500),
                              current_status  VARCHAR(20) NOT NULL CHECK (current_status IN
                                                                          ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE')),
                              applied_date    DATE NOT NULL,
                              match_score     NUMERIC(5,2),
                              deleted         BOOLEAN NOT NULL DEFAULT FALSE,
                              version         BIGINT NOT NULL DEFAULT 0,
                              created_at      TIMESTAMP NOT NULL,
                              updated_at      TIMESTAMP NOT NULL
);

CREATE INDEX idx_applications_company ON applications (company);
CREATE INDEX idx_applications_status ON applications (current_status);
CREATE INDEX idx_applications_applied_date ON applications (applied_date);

CREATE TABLE application_status_history (
                                            id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
                                            old_status     VARCHAR(20),
                                            new_status     VARCHAR(20) NOT NULL,
                                            changed_at     TIMESTAMP NOT NULL
);

CREATE INDEX idx_status_history_changed_at ON application_status_history (changed_at);
CREATE INDEX idx_status_history_application_id ON application_status_history (application_id);

-- Seed user. Username: rachit / Password: changeme123
-- IMPORTANT: this is a real, valid BCrypt hash, but change the password
-- before deploying anywhere beyond local dev. See README for how to
-- generate a new hash with Spring's BCryptPasswordEncoder.
INSERT INTO users (id, username, password_hash) VALUES
    (gen_random_uuid(), 'rachit', '$2b$10$uUf2MYE0SUsTb3sXHe7Bcughau.3SojjCq/uwuPmt41mOTbNde4/e');