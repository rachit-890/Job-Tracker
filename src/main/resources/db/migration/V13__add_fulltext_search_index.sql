-- V13__add_fulltext_search_index.sql
-- Adds PostgreSQL GIN full-text search capabilities to applications.
-- We index company, role, and jd_text together into a tsvector.

ALTER TABLE applications ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('english', coalesce(company, '') || ' ' || coalesce(role, '') || ' ' || coalesce(jd_text, ''))
    ) STORED;

CREATE INDEX idx_applications_fts ON applications USING GIN (search_vector);
