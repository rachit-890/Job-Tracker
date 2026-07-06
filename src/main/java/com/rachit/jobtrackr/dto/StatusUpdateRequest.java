package com.rachit.jobtrackr.dto;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "newStatus is required") ApplicationStatus newStatus
) {
}
