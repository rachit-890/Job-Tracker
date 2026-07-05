package com.proj.jobtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Singleton row maintained by the AnalyticsConsumer.
 * Dashboard reads from here instead of scanning raw application rows.
 * The fixed ID matches the seed row in V3 migration.
 */
@Entity
@Table(name = "analytics_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSnapshot {

    public static final UUID SINGLETON_ID =
            UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(name = "total_applications", nullable = false)
    private int totalApplications;

    @Column(name = "applied_count", nullable = false)
    private int appliedCount;

    @Column(name = "screening_count", nullable = false)
    private int screeningCount;

    @Column(name = "interview_count", nullable = false)
    private int interviewCount;

    @Column(name = "offer_count", nullable = false)
    private int offerCount;

    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount;

    @Column(name = "stale_count", nullable = false)
    private int staleCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
