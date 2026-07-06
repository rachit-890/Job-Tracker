package com.rachit.jobtrackr.event;

import java.time.Instant;
import java.util.UUID;

public record ReminderCreatedPayload(
        UUID reminderId,
        UUID applicationId,
        String company,
        String role,
        Instant remindAt
) {
}
