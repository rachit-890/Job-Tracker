package com.rachit.jobtrackr.dto;


import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        Instant changedAt
) {
    public static StatusHistoryResponse from(ApplicationStatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getOldStatus(), h.getNewStatus(), h.getChangedAt());
    }
}