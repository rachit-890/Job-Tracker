package com.proj.jobtracker.repository;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

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
              and a.currentStatus = com.proj.jobtracker.domain.ApplicationStatus.APPLIED
              and a.appliedDate <= :cutoffDate
            """)
    Page<JobApplication> findStaleCandidates(@Param("cutoffDate") LocalDate cutoffDate, Pageable pageable);
}
