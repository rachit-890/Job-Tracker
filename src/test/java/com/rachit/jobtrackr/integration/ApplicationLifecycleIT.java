package com.rachit.jobtrackr.integration;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.CreateApplicationRequest;
import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.entity.ApplicationStatusHistory;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.exception.InvalidStatusTransitionException;
import com.rachit.jobtrackr.repository.AnalyticsSnapshotRepository;
import com.rachit.jobtrackr.repository.ApplicationStatusHistoryRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import com.rachit.jobtrackr.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * End-to-end integration test for the full application lifecycle.
 * Uses real PostgreSQL (Testcontainers) + real Kafka for event flow.
 *
 * Tests:
 * 1. Create application → verify DB state + initial status
 * 2. Analytics snapshot updated by Kafka consumer
 * 3. Status transitions through the pipeline
 * 4. Status history audit trail
 * 5. Invalid transition rejection
 * 6. Soft delete
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationLifecycleIT extends IntegrationTestBase {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository historyRepository;

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    private static UUID applicationId;

    @Test
    @Order(1)
    @DisplayName("Create application → persists with APPLIED status and publishes event")
    void createApplication_shouldPersistAndPublishEvent() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "Google",
                "Software Engineer",
                "Build distributed systems at scale",
                "v2.1",
                "Experienced Java developer with 5 years...",
                "https://careers.google.com/jobs/123",
                LocalDate.now()
        );

        JobApplication created = applicationService.create(request);
        applicationId = created.getId();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCompany()).isEqualTo("Google");
        assertThat(created.getRole()).isEqualTo("Software Engineer");
        assertThat(created.getCurrentStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(created.isDeleted()).isFalse();

        // Verify persisted in DB
        Optional<JobApplication> fromDb = applicationRepository.findByIdAndDeletedFalse(applicationId);
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getCompany()).isEqualTo("Google");
    }

    @Test
    @Order(2)
    @DisplayName("Analytics snapshot incremented by Kafka consumer after application creation")
    void analyticsSnapshot_shouldIncrementAfterCreate() {
        // The AnalyticsConsumer processes the ApplicationCreatedEvent asynchronously.
        // We poll until the snapshot reflects the new application.
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            AnalyticsSnapshot snapshot = analyticsSnapshotRepository
                    .findById(AnalyticsSnapshot.SINGLETON_ID)
                    .orElseThrow();
            assertThat(snapshot.getTotalApplications()).isGreaterThanOrEqualTo(1);
            assertThat(snapshot.getAppliedCount()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Status transition APPLIED → SCREENING succeeds")
    void updateStatus_appliedToScreening_shouldSucceed() {
        applicationService.updateStatus(applicationId, ApplicationStatus.SCREENING);

        JobApplication updated = applicationService.getById(applicationId);
        assertThat(updated.getCurrentStatus()).isEqualTo(ApplicationStatus.SCREENING);
    }

    @Test
    @Order(4)
    @DisplayName("Status transition SCREENING → INTERVIEW succeeds")
    void updateStatus_screeningToInterview_shouldSucceed() {
        applicationService.updateStatus(applicationId, ApplicationStatus.INTERVIEW);

        JobApplication updated = applicationService.getById(applicationId);
        assertThat(updated.getCurrentStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @Order(5)
    @DisplayName("Status transition INTERVIEW → OFFER succeeds")
    void updateStatus_interviewToOffer_shouldSucceed() {
        applicationService.updateStatus(applicationId, ApplicationStatus.OFFER);

        JobApplication updated = applicationService.getById(applicationId);
        assertThat(updated.getCurrentStatus()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    @Order(6)
    @DisplayName("Full status history audit trail is recorded")
    void statusHistory_shouldContainFullAuditTrail() {
        List<ApplicationStatusHistory> history =
                historyRepository.findByApplicationIdOrderByChangedAtAsc(applicationId);

        // Should have 4 entries: initial APPLIED + 3 transitions
        assertThat(history).hasSizeGreaterThanOrEqualTo(4);

        // First entry: null → APPLIED (initial creation)
        assertThat(history.get(0).getOldStatus()).isNull();
        assertThat(history.get(0).getNewStatus()).isEqualTo(ApplicationStatus.APPLIED);

        // Subsequent transitions
        assertThat(history.get(1).getOldStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.get(1).getNewStatus()).isEqualTo(ApplicationStatus.SCREENING);

        assertThat(history.get(2).getOldStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(history.get(2).getNewStatus()).isEqualTo(ApplicationStatus.INTERVIEW);

        assertThat(history.get(3).getOldStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(history.get(3).getNewStatus()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    @Order(7)
    @DisplayName("Invalid transition OFFER → APPLIED is rejected")
    void updateStatus_offerToApplied_shouldThrow() {
        assertThatThrownBy(() ->
                applicationService.updateStatus(applicationId, ApplicationStatus.APPLIED)
        ).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @Order(8)
    @DisplayName("Soft delete marks application as deleted")
    void softDelete_shouldMarkAsDeleted() {
        applicationService.softDelete(applicationId);

        // findByIdAndDeletedFalse should return empty
        Optional<JobApplication> result = applicationRepository.findByIdAndDeletedFalse(applicationId);
        assertThat(result).isEmpty();

        // But the row still exists in DB
        Optional<JobApplication> raw = applicationRepository.findById(applicationId);
        assertThat(raw).isPresent();
        assertThat(raw.get().isDeleted()).isTrue();
    }
}
