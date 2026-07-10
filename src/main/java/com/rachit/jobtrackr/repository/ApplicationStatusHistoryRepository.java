package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);

    // Count new applications created in a date range (old_status = null means initial APPLIED entry)
    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus is null
              and cast(h.changedAt as LocalDate) >= :from
              and cast(h.changedAt as LocalDate) <= :to
            """)
    int countNewApplicationsInRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Count all status changes (any transition) in a date range
    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus is not null
              and cast(h.changedAt as LocalDate) >= :from
              and cast(h.changedAt as LocalDate) <= :to
            """)
    int countStatusChangesInRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Count responses = transitions OUT of APPLIED (company replied in some form)
    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus = com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
              and cast(h.changedAt as LocalDate) >= :from
              and cast(h.changedAt as LocalDate) <= :to
            """)
    int countResponsesInRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}