package com.proj.jobtracker.dto;


import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.entity.ApplicationStatusHistory;

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