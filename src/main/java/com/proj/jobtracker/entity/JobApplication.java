package com.proj.jobtracker.entity;

import com.proj.jobtracker.domain.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "resume_version")
    private String resumeVersion;

    @Column(name = "source_url")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private ApplicationStatus currentStatus;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(nullable = false)
    private boolean deleted;

    // Optimistic locking: prevents lost updates when two requests modify
    // the same application's status concurrently.
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (currentStatus == null) {
            currentStatus = ApplicationStatus.APPLIED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
