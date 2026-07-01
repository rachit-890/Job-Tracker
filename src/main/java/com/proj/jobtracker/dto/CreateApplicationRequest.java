package com.proj.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank(message = "company is required") String company,
        @NotBlank(message = "role is required") String role,
        String jdText,
        String resumeVersion,
        String sourceUrl,
        @NotNull(message = "appliedDate is required") LocalDate appliedDate
) {
}
