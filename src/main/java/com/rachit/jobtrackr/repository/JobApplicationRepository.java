package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, UUID>,
        JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByIdAndDeletedFalse(UUID id);

    Page<JobApplication> findAllByDeletedFalse(Pageable pageable);

    Page<JobApplication> findByCompanyIgnoreCaseAndDeletedFalse(String company, Pageable pageable);

    Page<JobApplication> findByCurrentStatusAndDeletedFalse(ApplicationStatus status, Pageable pageable);

    @Query(value = """
            select * from applications a
            where a.deleted = false
              and a.search_vector @@ plainto_tsquery('english', :q)
            """, nativeQuery = true)
    Page<JobApplication> search(@Param("q") String query, Pageable pageable);

    @Query("""
            select a from JobApplication a
            where a.deleted = false
              and a.currentStatus = :status
              and a.appliedDate <= :cutoffDate
            """)
    Page<JobApplication> findStaleCandidates(
            @Param("status") ApplicationStatus status,
            @Param("cutoffDate") LocalDate cutoffDate,
            Pageable pageable);

    // Average days from appliedDate to first response
    @Query(value = """
            select avg(extract(epoch from (h.changed_at - a.applied_date::timestamp)) / 86400)
            from applications a
            join application_status_history h on h.application_id = a.id
            where a.deleted = false
              and h.old_status = 'APPLIED'
              and h.new_status != 'STALE'
            """, nativeQuery = true)
    Double computeAvgTimeToResponse();

    // Resume performance — native query returns Object[] rows, mapped in service
    // Returns: [resumeVersion, totalCount, callbackCount]
    @Query(value = """
            select
                a.resume_version,
                count(*) as total,
                sum(case when a.current_status not in ('APPLIED','STALE') then 1 else 0 end) as callbacks
            from applications a
            where a.deleted = false and a.resume_version is not null
            group by a.resume_version
            order by count(*) desc
            """, nativeQuery = true)
    List<Object[]> computeResumePerformanceRaw();

    // Company analytics — native query returns Object[] rows, mapped in service
    // Returns: [company, totalCount, callbackCount]
    @Query(value = """
            select
                a.company,
                count(*) as total,
                sum(case when a.current_status not in ('APPLIED','STALE') then 1 else 0 end) as callbacks
            from applications a
            where a.deleted = false
            group by a.company
            order by count(*) desc
            """, nativeQuery = true)
    List<Object[]> computeCompanyAnalyticsRaw();

    // Trend: applications created in a time range
    @Query("""
            select count(a) from JobApplication a
            where a.deleted = false
              and a.createdAt >= :from
              and a.createdAt < :to
            """)
    int countCreatedInRange(@Param("from") Instant from, @Param("to") Instant to);

    // Trend: responses (status moved out of APPLIED) in a time range
    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus = com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
              and h.changedAt >= :from
              and h.changedAt < :to
            """)
    int countResponsesInRange(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Aggregates applications by day of week using PostgreSQL's EXTRACT(DOW).
     * DOW values: 0=Sunday, 1=Monday, ..., 6=Saturday.
     * Used by the day-of-week heatmap on the analytics dashboard.
     */
    @Query(value = """
            select extract(dow from a.applied_date) as dow, count(*) as cnt
            from applications a
            where a.deleted = false
            group by extract(dow from a.applied_date)
            order by dow
            """, nativeQuery = true)
    List<Object[]> countByDayOfWeek();
}