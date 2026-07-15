-- V10__add_application_notes.sql
-- Timestamped notes for each application.
-- Examples: "Called HR", "Met recruiter at event", "Sent follow-up email"
-- Complements the automatic status_history timeline with user-authored context.

CREATE TABLE application_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notes_application_id ON application_notes(application_id);
