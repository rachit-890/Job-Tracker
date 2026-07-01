package com.proj.jobtracker.service;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.domain.StatusTransitionPolicy;
import com.proj.jobtracker.dto.CreateApplicationRequest;
import com.proj.jobtracker.dto.UpdateApplicationRequest;
import com.proj.jobtracker.entity.ApplicationStatusHistory;
import com.proj.jobtracker.entity.JobApplication;
import com.proj.jobtracker.exception.InvalidStatusTransitionException;
import com.proj.jobtracker.exception.ResourceNotFoundException;
import com.proj.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.proj.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public JobApplication create(CreateApplicationRequest request) {
        JobApplication application = JobApplication.builder()
                .company(request.company())
                .role(request.role())
                .jdText(request.jdText())
                .resumeVersion(request.resumeVersion())
                .sourceUrl(request.sourceUrl())
                .appliedDate(request.appliedDate())
                .currentStatus(ApplicationStatus.APPLIED)
                .deleted(false)
                .build();

        JobApplication saved = applicationRepository.save(application);

        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(saved.getId())
                .oldStatus(null)
                .newStatus(ApplicationStatus.APPLIED)
                .changedAt(Instant.now())
                .build());

        // NOTE: Phase 3 will replace this direct save with publishing an
        // ApplicationCreatedEvent so AI tag extraction / match scoring run
        // asynchronously via a Kafka consumer instead of inline here.

        return saved;
    }

    public JobApplication getById(UUID id) {
        return applicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    public Page<JobApplication> list(Pageable pageable) {
        return applicationRepository.findAllByDeletedFalse(pageable);
    }

    public Page<JobApplication> search(String query, Pageable pageable) {
        return applicationRepository.search(query, pageable);
    }

    public Page<JobApplication> byCompany(String company, Pageable pageable) {
        return applicationRepository.findByCompanyIgnoreCaseAndDeletedFalse(company, pageable);
    }

    public Page<JobApplication> byStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByCurrentStatusAndDeletedFalse(status, pageable);
    }

    public Page<JobApplication> stale(int staleAfterDays, Pageable pageable) {
        LocalDate cutoff = LocalDate.now().minusDays(staleAfterDays);
        // FIX: pass ApplicationStatus.APPLIED as a parameter (matching the fixed repository method)
        return applicationRepository.findStaleCandidates(ApplicationStatus.APPLIED, cutoff, pageable);
    }

    @Transactional
    public JobApplication update(UUID id, UpdateApplicationRequest request) {
        JobApplication application = getById(id);

        if (request.company() != null) application.setCompany(request.company());
        if (request.role() != null) application.setRole(request.role());
        if (request.jdText() != null) application.setJdText(request.jdText());
        if (request.resumeVersion() != null) application.setResumeVersion(request.resumeVersion());
        if (request.sourceUrl() != null) application.setSourceUrl(request.sourceUrl());

        return applicationRepository.save(application);
    }

    @Transactional
    public JobApplication updateStatus(UUID id, ApplicationStatus newStatus) {
        JobApplication application = getById(id);
        ApplicationStatus currentStatus = application.getCurrentStatus();

        if (!StatusTransitionPolicy.isValidTransition(currentStatus, newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from %s to %s. Allowed next statuses: %s"
                            .formatted(currentStatus, newStatus,
                                    StatusTransitionPolicy.allowedNextStatuses(currentStatus)));
        }

        application.setCurrentStatus(newStatus);
        JobApplication saved = applicationRepository.save(application);

        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(saved.getId())
                .oldStatus(currentStatus)
                .newStatus(newStatus)
                .changedAt(Instant.now())
                .build());

        // NOTE: Phase 3 will replace this direct write with publishing a
        // StatusChangedEvent, fanned out to analytics, reminder, and
        // notification consumers.

        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        JobApplication application = getById(id);
        application.setDeleted(true);
        applicationRepository.save(application);
    }
}