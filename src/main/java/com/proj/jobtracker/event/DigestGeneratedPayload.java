package com.proj.jobtracker.event;

import java.time.LocalDate;

public record DigestGeneratedPayload(
        LocalDate weekStart,
        LocalDate weekEnd,
        int newApplications,
        int statusChanges,
        int responseCount
) {
}