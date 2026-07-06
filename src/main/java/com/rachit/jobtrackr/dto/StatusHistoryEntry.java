package com.rachit.jobtrackr.dto;


import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.ApplicationStatusHistory;

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
