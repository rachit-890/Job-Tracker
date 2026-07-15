package com.rachit.jobtrackr.integration;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.CreateApplicationRequest;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.exception.InvalidStatusTransitionException;
import com.rachit.jobtrackr.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the status state machine with real DB persistence.
 *
 * Tests every valid and invalid transition defined in StatusTransitionPolicy
 * against a real PostgreSQL database, verifying that:
 * - Valid transitions succeed and persist correctly
 * - Invalid transitions throw InvalidStatusTransitionException
 * - Terminal states (OFFER, REJECTED) allow no outgoing transitions
 * - STALE → SCREENING recovery path works
 */
class StatusTransitionPolicyIT extends IntegrationTestBase {

    @Autowired
    private ApplicationService applicationService;

    /**
     * Helper: creates a fresh application and advances it to the desired starting status.
     */
    private JobApplication createAppAtStatus(ApplicationStatus targetStatus) {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "TransitionTest-" + targetStatus,
                "Engineer",
                "Test JD",
                null, null, null,
                LocalDate.now()
        );
        JobApplication app = applicationService.create(request);

        // Walk the application through the pipeline to reach the target status
        if (targetStatus == ApplicationStatus.APPLIED) return app;

        if (targetStatus == ApplicationStatus.STALE) {
            applicationService.updateStatus(app.getId(), ApplicationStatus.STALE);
            return applicationService.getById(app.getId());
        }

        if (targetStatus == ApplicationStatus.REJECTED) {
            applicationService.updateStatus(app.getId(), ApplicationStatus.REJECTED);
            return applicationService.getById(app.getId());
        }

        applicationService.updateStatus(app.getId(), ApplicationStatus.SCREENING);
        if (targetStatus == ApplicationStatus.SCREENING) return applicationService.getById(app.getId());

        applicationService.updateStatus(app.getId(), ApplicationStatus.INTERVIEW);
        if (targetStatus == ApplicationStatus.INTERVIEW) return applicationService.getById(app.getId());

        applicationService.updateStatus(app.getId(), ApplicationStatus.OFFER);
        return applicationService.getById(app.getId());
    }

    // ── Valid transitions ──────────────────────────────────────────────────

    @ParameterizedTest(name = "Valid: {0} → {1}")
    @CsvSource({
            "APPLIED,    SCREENING",
            "APPLIED,    REJECTED",
            "APPLIED,    STALE",
            "SCREENING,  INTERVIEW",
            "SCREENING,  REJECTED",
            "INTERVIEW,  OFFER",
            "INTERVIEW,  REJECTED"
    })
    @DisplayName("Valid transitions succeed and persist correctly")
    void validTransitions_shouldSucceed(ApplicationStatus from, ApplicationStatus to) {
        JobApplication app = createAppAtStatus(from);

        applicationService.updateStatus(app.getId(), to);

        JobApplication updated = applicationService.getById(app.getId());
        assertThat(updated.getCurrentStatus()).isEqualTo(to);
    }

    @Test
    @DisplayName("STALE → SCREENING recovery transition succeeds")
    void staleToScreening_shouldSucceed() {
        JobApplication app = createAppAtStatus(ApplicationStatus.STALE);

        applicationService.updateStatus(app.getId(), ApplicationStatus.SCREENING);

        JobApplication updated = applicationService.getById(app.getId());
        assertThat(updated.getCurrentStatus()).isEqualTo(ApplicationStatus.SCREENING);
    }

    @Test
    @DisplayName("STALE → REJECTED transition succeeds")
    void staleToRejected_shouldSucceed() {
        JobApplication app = createAppAtStatus(ApplicationStatus.STALE);

        applicationService.updateStatus(app.getId(), ApplicationStatus.REJECTED);

        JobApplication updated = applicationService.getById(app.getId());
        assertThat(updated.getCurrentStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    // ── Invalid transitions ────────────────────────────────────────────────

    @ParameterizedTest(name = "Invalid: {0} → {1}")
    @CsvSource({
            "APPLIED,    INTERVIEW",
            "APPLIED,    OFFER",
            "SCREENING,  APPLIED",
            "SCREENING,  OFFER",
            "SCREENING,  STALE",
            "INTERVIEW,  APPLIED",
            "INTERVIEW,  SCREENING",
            "INTERVIEW,  STALE"
    })
    @DisplayName("Invalid transitions throw InvalidStatusTransitionException")
    void invalidTransitions_shouldThrow(ApplicationStatus from, ApplicationStatus to) {
        JobApplication app = createAppAtStatus(from);

        assertThatThrownBy(() ->
                applicationService.updateStatus(app.getId(), to)
        ).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Terminal state OFFER allows no outgoing transitions")
    void offer_shouldAllowNoTransitions() {
        JobApplication app = createAppAtStatus(ApplicationStatus.OFFER);

        for (ApplicationStatus target : ApplicationStatus.values()) {
            if (target == ApplicationStatus.OFFER) continue;
            assertThatThrownBy(() ->
                    applicationService.updateStatus(app.getId(), target)
            ).isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("Terminal state REJECTED allows no outgoing transitions")
    void rejected_shouldAllowNoTransitions() {
        JobApplication app = createAppAtStatus(ApplicationStatus.REJECTED);

        for (ApplicationStatus target : ApplicationStatus.values()) {
            if (target == ApplicationStatus.REJECTED) continue;
            assertThatThrownBy(() ->
                    applicationService.updateStatus(app.getId(), target)
            ).isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("Self-transition (same status) is rejected")
    void selfTransition_shouldBeRejected() {
        JobApplication app = createAppAtStatus(ApplicationStatus.APPLIED);

        assertThatThrownBy(() ->
                applicationService.updateStatus(app.getId(), ApplicationStatus.APPLIED)
        ).isInstanceOf(InvalidStatusTransitionException.class);
    }
}
