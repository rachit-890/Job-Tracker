package com.proj.jobtracker.dto;


import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.entity.ApplicationStatusHistory;
import com.proj.jobtracker.entity.ApplicationTag;
import com.proj.jobtracker.entity.JobApplication;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full detail response for GET /applications/{id}.
 * Includes status history and tags — not returned in list endpoints
 * to avoid fetching large amounts of data for every row.
 */
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
        List<StatusHistoryResponse> statusHistory,
        List<String> tags
) {
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
                history.stream().map(StatusHistoryResponse::from).toList(),
                tags.stream().map(ApplicationTag::getTag).toList()
        );
    }
}
