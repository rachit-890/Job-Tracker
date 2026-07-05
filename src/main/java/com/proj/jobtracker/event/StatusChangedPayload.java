package com.proj.jobtracker.event;


import com.proj.jobtracker.domain.ApplicationStatus;

import java.util.UUID;

public record StatusChangedPayload(
        UUID applicationId,
        String company,
        String role,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus
) {
}
