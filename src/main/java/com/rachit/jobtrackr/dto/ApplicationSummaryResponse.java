package com.rachit.jobtrackr.dto;


import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight response for list endpoints.
 * Excludes jdText (can be kilobytes) — fetch the full detail via GET /{id}.
 */
public record ApplicationSummaryResponse(
        UUID id,
        String company,
        String role,
        String resumeVersion,
        ApplicationStatus currentStatus,
        LocalDate appliedDate,
        Double matchScore,
        Long version
) {
    public static ApplicationSummaryResponse from(JobApplication app) {
        return new ApplicationSummaryResponse(
                app.getId(),
                app.getCompany(),
                app.getRole(),
                app.getResumeVersion(),
                app.getCurrentStatus(),
                app.getAppliedDate(),
                app.getMatchScore(),
                app.getVersion()
        );
    }
}