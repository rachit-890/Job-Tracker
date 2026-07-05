package com.proj.jobtracker.service;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.domain.StatusTransitionPolicy;
import com.proj.jobtracker.dto.ApplicationDetailResponse;
import com.proj.jobtracker.dto.CreateApplicationRequest;
import com.proj.jobtracker.dto.UpdateApplicationRequest;
import com.proj.jobtracker.entity.ApplicationStatusHistory;
import com.proj.jobtracker.entity.ApplicationTag;
import com.proj.jobtracker.entity.JobApplication;
import com.proj.jobtracker.event.ApplicationCreatedPayload;
import com.proj.jobtracker.event.StatusChangedPayload;
import com.proj.jobtracker.exception.InvalidStatusTransitionException;
import com.proj.jobtracker.exception.ResourceNotFoundException;
import com.proj.jobtracker.repository.ApplicationSpecification;
import com.proj.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.proj.jobtracker.repository.ApplicationTagRepository;
import com.proj.jobtracker.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationTagRepository tagRepository;
    private final EventPublisher eventPublisher;

    public ApplicationService(JobApplicationRepository applicationRepository,
                               ApplicationStatusHistoryRepository historyRepository,
                               ApplicationTagRepository tagRepository,
                               EventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
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

        ApplicationCreatedPayload payload = new ApplicationCreatedPayload(
                saved.getId(),
                saved.getCompany(),
                saved.getRole(),
                saved.getJdText(),
                saved.getResumeVersion(),
                saved.getAppliedDate()
        );
        publishAfterCommit(() -> eventPublisher.publishApplicationCreated(payload));

        return saved;
    }

    public JobApplication getById(UUID id) {
        return applicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getDetail(UUID id) {
        JobApplication application = getById(id);
        List<ApplicationStatusHistory> history =
                historyRepository.findByApplicationIdOrderByChangedAtAsc(id);
        List<ApplicationTag> tags = tagRepository.findByApplicationId(id);
        return ApplicationDetailResponse.from(application, history, tags);
    }

    public Page<JobApplication> list(
            ApplicationStatus status,
            String company,
            LocalDate appliedDateFrom,
            LocalDate appliedDateTo,
            Pageable pageable) {

        Specification<JobApplication> spec = ApplicationSpecification.notDeleted();

        if (status != null) {
            spec = spec.and(ApplicationSpecification.hasStatus(status));
        }
        if (company != null && !company.isBlank()) {
            spec = spec.and(ApplicationSpecification.companyContains(company));
        }
        if (appliedDateFrom != null) {
            spec = spec.and(ApplicationSpecification.appliedOnOrAfter(appliedDateFrom));
        }
        if (appliedDateTo != null) {
            spec = spec.and(ApplicationSpecification.appliedOnOrBefore(appliedDateTo));
        }

        return applicationRepository.findAll(spec, pageable);
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
        ApplicationStatus oldStatus = application.getCurrentStatus();

        if (!StatusTransitionPolicy.isValidTransition(oldStatus, newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from %s to %s. Allowed next statuses: %s"
                            .formatted(oldStatus, newStatus,
                                    StatusTransitionPolicy.allowedNextStatuses(oldStatus)));
        }

        application.setCurrentStatus(newStatus);
        JobApplication saved = applicationRepository.save(application);

        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(saved.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(Instant.now())
                .build());

        StatusChangedPayload payload = new StatusChangedPayload(
                saved.getId(),
                saved.getCompany(),
                saved.getRole(),
                oldStatus,
                newStatus
        );
        publishAfterCommit(() -> eventPublisher.publishStatusChanged(payload));

        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        JobApplication application = getById(id);
        application.setDeleted(true);
        applicationRepository.save(application);
    }

    private void publishAfterCommit(Runnable publish) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}