package com.proj.jobtracker.service;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.entity.AnalyticsSnapshot;
import com.proj.jobtracker.repository.AnalyticsSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.proj.jobtracker.domain.ApplicationStatus.REJECTED;

/**
 * Maintains the analytics_snapshot singleton row.
 * Called by the AnalyticsConsumer — not directly by request-path code.
 *
 * Redis key for the cached snapshot. When invalidated here, the next
 * dashboard read (Phase 6 frontend) will repopulate it from the DB.
 */
@Service
public class AnalyticsService {

    public static final String ANALYTICS_CACHE_KEY = "analytics:snapshot";

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AnalyticsService(AnalyticsSnapshotRepository snapshotRepository,
                            RedisTemplate<String, Object> redisTemplate) {
        this.snapshotRepository = snapshotRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Called when a new application is created (ApplicationCreatedEvent).
     * Increments total and the APPLIED bucket.
     */
    @Transactional
    public void onApplicationCreated() {
        AnalyticsSnapshot snapshot = getSnapshot();
        snapshot.setTotalApplications(snapshot.getTotalApplications() + 1);
        snapshot.setAppliedCount(snapshot.getAppliedCount() + 1);
        snapshot.setComputedAt(Instant.now());
        snapshotRepository.save(snapshot);
        invalidateCache();
    }

    /**
     * Called when a status transition occurs (StatusChangedEvent).
     * Decrements the old status bucket, increments the new one.
     */
    @Transactional
    public void onStatusChanged(ApplicationStatus oldStatus, ApplicationStatus newStatus) {
        AnalyticsSnapshot snapshot = getSnapshot();
        decrement(snapshot, oldStatus);
        increment(snapshot, newStatus);
        snapshot.setComputedAt(Instant.now());
        snapshotRepository.save(snapshot);
        invalidateCache();
    }

    public AnalyticsSnapshot getSnapshot() {
        return snapshotRepository.findById(AnalyticsSnapshot.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Analytics snapshot row missing — check V3 migration"));
    }

    private void invalidateCache() {
        redisTemplate.delete(ANALYTICS_CACHE_KEY);
        log.debug("Analytics cache invalidated");
    }

    private void increment(AnalyticsSnapshot s, ApplicationStatus status) {
        switch (status) {
            case APPLIED    -> s.setAppliedCount(s.getAppliedCount() + 1);
            case SCREENING  -> s.setScreeningCount(s.getScreeningCount() + 1);
            case INTERVIEW  -> s.setInterviewCount(s.getInterviewCount() + 1);
            case OFFER      -> s.setOfferCount(s.getOfferCount() + 1);
            case REJECTED   -> s.setRejectedCount(s.getRejectedCount() + 1);
            case STALE      -> s.setStaleCount(s.getStaleCount() + 1);
        }
    }

    private void decrement(AnalyticsSnapshot s, ApplicationStatus status) {
        if (status == null) return; // initial APPLIED → no old status to decrement
        switch (status) {
            case APPLIED    -> s.setAppliedCount(Math.max(0, s.getAppliedCount() - 1));
            case SCREENING  -> s.setScreeningCount(Math.max(0, s.getScreeningCount() - 1));
            case INTERVIEW  -> s.setInterviewCount(Math.max(0, s.getInterviewCount() - 1));
            case OFFER      -> s.setOfferCount(Math.max(0, s.getOfferCount() - 1));
            case REJECTED   -> s.setRejectedCount(Math.max(0, s.getRejectedCount() - 1));
            case STALE      -> s.setStaleCount(Math.max(0, s.getStaleCount() - 1));
        }
    }
}
