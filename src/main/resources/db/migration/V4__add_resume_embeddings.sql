-- V4__add_resume_embeddings.sql
-- Stores computed Gemini embeddings keyed by resume content hash.
-- Why content hash and not version label?
-- If "Resume v2" is edited but the name stays the same, a label-keyed cache
-- would silently reuse a stale embedding. Hashing the content guarantees
-- the embedding always matches what was actually submitted.
-- Also stores which embedding model generated each vector so we can
-- identify and recompute stale embeddings if Google updates the model.

CREATE TABLE resume_embeddings (
                                   resume_hash      VARCHAR(64)  PRIMARY KEY,  -- SHA-256 hex of resume text
                                   resume_version   VARCHAR(100),              -- human-readable label, informational only
                                   embedding_model  VARCHAR(100) NOT NULL,     -- e.g. "text-embedding-004"
                                   embedding_vector TEXT         NOT NULL,     -- JSON array of floats
                                   generated_at     TIMESTAMP    NOT NULL
);