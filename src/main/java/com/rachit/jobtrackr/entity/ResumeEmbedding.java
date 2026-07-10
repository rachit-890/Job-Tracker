package com.rachit.jobtrackr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "resume_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeEmbedding {

    // SHA-256 hex of the resume text — keyed on content, not version label.
    // If the user edits "Resume v2" without renaming it, the hash changes
    // And we compute a fresh embedding rather than silently reusing the stale one.
    @Id
    @Column(name = "resume_hash", nullable = false)
    private String resumeHash;

    @Column(name = "resume_version")
    private String resumeVersion;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    // JSON array of floats serialized with Jackson — stored as TEXT.
    // Using TEXT instead of VECTOR type avoids the pgvector extension dependency,
    // which adds operational complexity for no benefit at our data volume.
    @Column(name = "embedding_vector", nullable = false, columnDefinition = "TEXT")
    private String embeddingVector;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}