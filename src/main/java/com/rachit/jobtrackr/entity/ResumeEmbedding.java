package com.rachit.jobtrackr.entity;

import com.rachit.jobtrackr.entity.converter.FloatArrayConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "resume_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResumeEmbedding {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "resume_hash", nullable = false)
    private String resumeHash;

    @Column(name = "resume_version")
    private String resumeVersion;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    // Stored as JSON array of floats in a TEXT column via FloatArrayConverter.
    // This is cleaner than raw String serialization scattered across service
    // classes — the conversion is now a single, well-defined place.
    // Upgrade path: swap FloatArrayConverter for a pgvector mapping when
    // vector similarity search in SQL becomes a requirement.
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding_vector", nullable = false, columnDefinition = "TEXT")
    private float[] embeddingVector;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}