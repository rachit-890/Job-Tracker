-- V12__add_resume_versions.sql
-- Resume Version Manager.
-- Elevates "resume_version" from a simple string label on the application
-- to a proper entity with text content.

CREATE TABLE resume_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label           VARCHAR(100) NOT NULL UNIQUE,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Seed an initial "default" version if needed, or leave empty.
