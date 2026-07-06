package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank(message = "company is required")
        @Size(max = 255, message = "company must be 255 characters or fewer")
        String company,

        @NotBlank(message = "role is required")
        @Size(max = 255, message = "role must be 255 characters or fewer")
        String role,

        String jdText,

        @Size(max = 100, message = "resumeVersion must be 100 characters or fewer")
        String resumeVersion,

        // FIX: validate URL format if provided
        @URL(message = "sourceUrl must be a valid URL")
        @Size(max = 500, message = "sourceUrl must be 500 characters or fewer")
        String sourceUrl,

        // FIX: reject future application dates — you can't have applied tomorrow
        @NotNull(message = "appliedDate is required")
        @PastOrPresent(message = "appliedDate cannot be in the future")
        LocalDate appliedDate
) {
}