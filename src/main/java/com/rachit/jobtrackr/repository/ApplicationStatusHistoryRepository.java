package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);

    // FIX: compare Instant ranges directly instead of casting changedAt to LocalDate.
    //
    // Why does casting hurt performance?
    // cast(h.changedAt as LocalDate) applies a function to every row's changedAt column.
    // Databases cannot use an index on changedAt when a function wraps it — the planner
    // must evaluate the function per row, resulting in a full table scan.
    //
    // Why does Instant range comparison allow index usage?
    // Comparing h.changedAt >= :fromInstant AND h.changedAt < :toInstant is a
    // straight range predicate on the raw column — the index on changedAt can be
    // used directly (index range scan instead of sequential scan).
    //
    // The WeeklyDigestJob passes LocalDate values; we convert to Instant at midnight UTC
    // in the service layer before calling these methods.

    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus is null
              and h.changedAt >= :fromInstant
              and h.changedAt < :toInstant
            """)
    int countNewApplicationsInRange(
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant);

    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus is not null
              and h.changedAt >= :fromInstant
              and h.changedAt < :toInstant
            """)
    int countStatusChangesInRange(
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant);

    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus = com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
              and h.changedAt >= :fromInstant
              and h.changedAt < :toInstant
            """)
    int countResponsesInRange(
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant);

    /**
     * Counts status transitions grouped by (oldStatus → newStatus) pair.
     * Used by the Sankey status-flow diagram on the analytics dashboard.
     * Excludes the initial APPLIED transition (oldStatus is null) since the
     * Sankey starts from the APPLIED node using the analytics snapshot count.
     */
    @Query("""
            select h.oldStatus, h.newStatus, count(h)
            from ApplicationStatusHistory h
            where h.oldStatus is not null
            group by h.oldStatus, h.newStatus
            """)
    List<Object[]> countTransitionsByStatusPair();
}