-- V4__add_resume_embeddings.sql
-- Stores cached Gemini embedding vectors for resumes.

CREATE TABLE resume_embeddings (
                                   resume_hash VARCHAR(64) PRIMARY KEY,
                                   resume_version VARCHAR(100),
                                   embedding_model VARCHAR(100) NOT NULL,
                                   embedding_version VARCHAR(50) NOT NULL,
                                   embedding_vector TEXT NOT NULL,
                                   generated_at TIMESTAMP NOT NULL
);