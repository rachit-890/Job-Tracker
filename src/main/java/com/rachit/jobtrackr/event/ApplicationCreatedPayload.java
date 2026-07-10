package com.rachit.jobtrackr.event;

import java.time.LocalDate;
import java.util.UUID;

public record ApplicationCreatedPayload(
        UUID applicationId,
        String company,
        String role,
        String jdText,
        String resumeVersion,
        String resumeText,      // added Phase 4 — needed by AiConsumer for embedding
        LocalDate appliedDate
) {
}