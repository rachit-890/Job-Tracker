package com.proj.jobtracker.dto;


import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.entity.ApplicationStatusHistory;

import java.time.Instant;

public record StatusHistoryEntry(
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        Instant changedAt
) {
    public static StatusHistoryEntry from(ApplicationStatusHistory h) {
        return new StatusHistoryEntry(h.getOldStatus(), h.getNewStatus(), h.getChangedAt());
    }
}
