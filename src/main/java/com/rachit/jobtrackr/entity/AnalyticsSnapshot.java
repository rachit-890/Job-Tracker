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
     * Derived field — computed from current snapshot state.
     * Response = any application that moved out of APPLIED status.
     * Not stored in DB — calculated on demand.
     */
    public double getResponseRate() {
        if (totalApplications == 0) return 0.0;
        int responded = screeningCount + interviewCount + offerCount + rejectedCount;
        return Math.round((responded * 100.0 / totalApplications) * 100.0) / 100.0;
    }
}