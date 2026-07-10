package com.rachit.jobtrackr.entity;

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
 * Singleton row that holds pre-aggregated job search analytics.
 *
 * Why a singleton?
 * Dashboard reads happen far more often than status changes.
 * Computing response rate, status breakdown, and totals from raw application
 * rows on every dashboard load would be expensive. Instead, the AnalyticsConsumer
 * maintains this single row, updating it on every relevant Kafka event.
 * Dashboard reads hit this row (optionally cached in Redis) — no live aggregation.
 *
 * The fixed ID (a0000000-0000-0000-0000-000000000001) is seeded by V3 migration.
 * AnalyticsService always updates this row — it never inserts a second one.
 *
 * Upgrade path: for time-series trend data (Phase 6+), add a separate
 * analytics_daily_snapshot table with one row per day rather than changing
 * this singleton structure.
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

    /**
     * Derived — not stored in DB, computed from current snapshot counts.
     * Response = any application that moved out of APPLIED status.
     */
    public double getResponseRate() {
        if (totalApplications == 0) return 0.0;
        int responded = screeningCount + interviewCount + offerCount + rejectedCount;
        return Math.round((responded * 100.0 / totalApplications) * 100.0) / 100.0;
    }
}