package com.rachit.jobtrackr.event;

import java.time.LocalDate;

public record DigestGeneratedPayload(
        LocalDate weekStart,
        LocalDate weekEnd,
        int newApplications,
        int statusChanges,
        int responseCount
) {
}