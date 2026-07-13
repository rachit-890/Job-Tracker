-- V5__remove_embedding_version_column.sql
ALTER TABLE resume_embeddings DROP COLUMN IF EXISTS embedding_version;