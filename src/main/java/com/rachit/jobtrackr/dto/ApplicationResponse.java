package com.rachit.jobtrackr.dto;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String company,
        String role,
        String jdText,
        String resumeVersion,
        String sourceUrl,
        ApplicationStatus currentStatus,
        LocalDate appliedDate,
        Double matchScore,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(JobApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCompany(),
                app.getRole(),
                app.getJdText(),
                app.getResumeVersion(),
                app.getSourceUrl(),
                app.getCurrentStatus(),
                app.getAppliedDate(),
                app.getMatchScore(),
                app.getVersion(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
