package com.rachit.jobtrackr.event;

import java.util.UUID;

public record ResumeScoredPayload(
        UUID applicationId,
        double matchScore,
        String resumeVersion
) {
}
