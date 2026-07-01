package com.proj.jobtracker.dto;

import com.proj.jobtracker.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "newStatus is required") ApplicationStatus newStatus
) {
}
