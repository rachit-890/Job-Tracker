package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record CaptureRequest(
        @NotBlank(message = "company is required")
        @Size(max = 255)
        String company,

        @NotBlank(message = "role is required")
        @Size(max = 255)
        String role,

        @URL(message = "sourceUrl must be a valid URL")
        @Size(max = 500)
        String sourceUrl,

        // Optional — defaults to today if not provided
        LocalDate appliedDate
) {
}