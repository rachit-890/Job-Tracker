package com.rachit.jobtrackr.event;


import com.rachit.jobtrackr.domain.ApplicationStatus;

import java.util.UUID;

public record StatusChangedPayload(
        UUID applicationId,
        String company,
        String role,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus
) {
}
