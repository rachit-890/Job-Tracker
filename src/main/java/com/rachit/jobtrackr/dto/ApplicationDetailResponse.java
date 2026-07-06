package com.rachit.jobtrackr.dto;


import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.ApplicationStatusHistory;
import com.rachit.jobtrackr.entity.ApplicationTag;
import com.rachit.jobtrackr.entity.JobApplication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApplicationDetailResponse(
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
        Instant updatedAt,
        List<String> tags,
        List<StatusHistoryEntry> statusHistory
) {
    // Parameter order: app, history, tags — history second, tags third.
    // ApplicationService.getDetail() must match this exact order.
    public static ApplicationDetailResponse from(
            JobApplication app,
            List<ApplicationStatusHistory> history,
            List<ApplicationTag> tags) {

        return new ApplicationDetailResponse(
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
                app.getUpdatedAt(),
                tags.stream().map(ApplicationTag::getTag).toList(),
                history.stream().map(StatusHistoryEntry::from).toList()
        );
    }
}