package com.rachit.jobtrackr.repository;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.CompanyAnalyticsResponse;
import com.rachit.jobtrackr.dto.ResumePerformanceResponse;
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

    @Query("""
            select a from JobApplication a
            where a.deleted = false
              and (lower(a.company) like lower(concat('%', :q, '%'))
                   or lower(a.role) like lower(concat('%', :q, '%')))
            """)
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

    // Average days between appliedDate and first response (any status change out of APPLIED)
    @Query(value = """
            select avg(extract(epoch from (h.changed_at - (a.applied_date::timestamp)))
                       / 86400)
            from applications a
            join application_status_history h on h.application_id = a.id
            where a.deleted = false
              and h.old_status = 'APPLIED'
              and h.new_status != 'STALE'
            """, nativeQuery = true)
    Double computeAvgTimeToResponse();

    // Callback rate per resume version (applications that moved past APPLIED / total)
    @Query(value = """
            select new com.rachit.jobtrackr.dto.ResumePerformanceResponse(
                a.resumeVersion,
                count(a),
                round(
                    (sum(case when a.currentStatus != com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
                              and a.currentStatus != com.rachit.jobtrackr.domain.ApplicationStatus.STALE
                         then 1.0 else 0.0 end)
                     / count(a)) * 100, 2)
            )
            from JobApplication a
            where a.deleted = false and a.resumeVersion is not null
            group by a.resumeVersion
            order by count(a) desc
            """)
    List<ResumePerformanceResponse> computeResumePerformance();

    // Company-wise stats
    @Query(value = """
            select new com.rachit.jobtrackr.dto.CompanyAnalyticsResponse(
                a.company,
                count(a),
                round(
                    (sum(case when a.currentStatus != com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
                              and a.currentStatus != com.rachit.jobtrackr.domain.ApplicationStatus.STALE
                         then 1.0 else 0.0 end)
                     / count(a)) * 100, 2),
                null
            )
            from JobApplication a
            where a.deleted = false
            group by a.company
            order by count(a) desc
            """)
    List<CompanyAnalyticsResponse> computeCompanyAnalytics();

    // Applications created in a time range (for trend chart)
    @Query("""
            select count(a) from JobApplication a
            where a.deleted = false
              and a.createdAt >= :from
              and a.createdAt < :to
            """)
    int countCreatedInRange(@Param("from") Instant from, @Param("to") Instant to);

    // Responses (status moved out of APPLIED) in a time range
    @Query("""
            select count(h) from ApplicationStatusHistory h
            where h.oldStatus = com.rachit.jobtrackr.domain.ApplicationStatus.APPLIED
              and h.changedAt >= :from
              and h.changedAt < :to
            """)
    int countResponsesInRange(@Param("from") Instant from, @Param("to") Instant to);
}