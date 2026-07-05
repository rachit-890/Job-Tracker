package com.proj.jobtracker.event;

import java.time.LocalDate;
import java.util.UUID;

public record ApplicationCreatedPayload(
        UUID applicationId,
        String company,
        String role,
        String jdText,
        String resumeVersion,
        LocalDate appliedDate
) {
}