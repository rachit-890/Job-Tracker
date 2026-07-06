package com.rachit.jobtrackr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "application_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_application_tag",
                columnNames = {"application_id", "tag"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(nullable = false, length = 100)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (source == null) {
            source = TagSource.MANUAL;
        }
        // Normalize to lowercase so "Java" and "java" don't both get stored
        if (tag != null) {
            tag = tag.toLowerCase().trim();
        }
    }
}
