package com.proj.jobtracker.service;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.domain.StatusTransitionPolicy;
import com.proj.jobtracker.entity.TagSource;
import com.proj.jobtracker.dto.ApplicationDetailResponse;
import com.proj.jobtracker.dto.CreateApplicationRequest;
import com.proj.jobtracker.dto.UpdateApplicationRequest;
import com.proj.jobtracker.entity.ApplicationStatusHistory;
import com.proj.jobtracker.entity.ApplicationTag;
import com.proj.jobtracker.entity.JobApplication;
import com.proj.jobtracker.exception.InvalidStatusTransitionException;
import com.proj.jobtracker.exception.ResourceNotFoundException;
import com.proj.jobtracker.repository.ApplicationStatusHistoryRepository;
import com.proj.jobtracker.repository.ApplicationTagRepository;
import com.proj.jobtracker.repository.JobApplicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationTagRepository tagRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              ApplicationStatusHistoryRepository historyRepository,
                              ApplicationTagRepository tagRepository) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.tagRepository = tagRepository;
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

        // NOTE: Phase 3 will publish ApplicationCreatedEvent here for async
        // AI tag extraction and match scoring via Kafka.

        return saved;
    }

    // Returns the entity — used internally by other service methods.
    public JobApplication getById(UUID id) {
        return applicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    // Returns the rich detail DTO — used by the GET /{id} controller endpoint.
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getDetail(UUID id) {
        JobApplication application = getById(id);
        List<ApplicationStatusHistory> history =
                historyRepository.findByApplicationIdOrderByChangedAtAsc(id);
        List<ApplicationTag> tags =
                tagRepository.findByApplicationIdOrderByTagAsc(id);
        return ApplicationDetailResponse.from(application, history, tags);
    }

    public Page<JobApplication> list(Pageable pageable) {
        return applicationRepository.findAllByDeletedFalse(pageable);
    }

    // Date-range filter — either or both params may be null (open-ended range).
    public Page<JobApplication> listByDateRange(LocalDate appliedAfter,
                                                LocalDate appliedBefore,
                                                Pageable pageable) {
        if (appliedAfter != null && appliedBefore != null && appliedAfter.isAfter(appliedBefore)) {
            throw new IllegalArgumentException("appliedAfter must not be after appliedBefore");
        }
        return applicationRepository.findByDateRange(appliedAfter, appliedBefore, pageable);
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

        // NOTE: Phase 3 will publish StatusChangedEvent here, fanned out
        // to analytics, reminder, and notification consumers.

        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        JobApplication application = getById(id);
        application.setDeleted(true);
        applicationRepository.save(application);
    }

    // ── Tag management ───────────────────────────────────────────────────────

    @Transactional
    public List<String> addTag(UUID applicationId, String rawTag) {
        // Confirm application exists before attempting insert.
        getById(applicationId);

        String normalized = rawTag.toLowerCase().trim();

        // Idempotent: if the tag already exists just return current tags.
        if (tagRepository.existsByApplicationIdAndTag(applicationId, normalized)) {
            return getTagsForApplication(applicationId);
        }

        try {
            tagRepository.save(ApplicationTag.builder()
                    .applicationId(applicationId)
                    .tag(normalized)
                    .source(TagSource.MANUAL)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Race condition: two concurrent requests added the same tag.
            // Safe to swallow — the tag is already there.
        }

        return getTagsForApplication(applicationId);
    }

    @Transactional
    public List<String> removeTag(UUID applicationId, String rawTag) {
        getById(applicationId);
        String normalized = rawTag.toLowerCase().trim();
        tagRepository.deleteByApplicationIdAndTag(applicationId, normalized);
        return getTagsForApplication(applicationId);
    }

    @Transactional(readOnly = true)
    public List<String> getTagsForApplication(UUID applicationId) {
        return tagRepository.findByApplicationIdOrderByTagAsc(applicationId)
                .stream()
                .map(ApplicationTag::getTag)
                .toList();
    }
}