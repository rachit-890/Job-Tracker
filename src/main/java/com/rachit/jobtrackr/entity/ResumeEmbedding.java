package com.rachit.jobtrackr.entity;

import jakarta.persistence.Column;
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

/**
 * Stores computed Gemini embeddings keyed by resume content hash.
 *
 * Why @Getter/@Setter/@EqualsAndHashCode instead of @Data?
 * @Data generates equals() and hashCode() based on ALL fields.
 * For JPA entities this causes two problems:
 * 1. Lazy-loaded fields trigger LazyInitializationException when
 *    accessed outside a transaction during equals/hashCode calls.
 * 2. Mutable fields in hashCode() break HashMap/HashSet invariants
 *    if the entity is mutated after being added to a collection.
 * Using @EqualsAndHashCode(onlyExplicitlyIncluded = true) with
 * only the @Id field is the safe pattern for JPA entities.
 */
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

    @Column(name = "embedding_vector", nullable = false, columnDefinition = "TEXT")
    private String embeddingVector;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}