package com.proj.jobtracker.event;

import java.util.UUID;

public record ResumeScoredPayload(
        UUID applicationId,
        double matchScore,
        String resumeVersion
) {
}
