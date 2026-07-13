package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailIngestRequest(
        // Raw email content — body + subject
        @NotBlank(message = "emailContent is required")
        @Size(max = 50000, message = "emailContent must be 50000 characters or fewer")
        String emailContent,

        // Optional Message-ID header — used as idempotency key if present.
        // If absent, SHA-256 of normalized emailContent is used.
        String messageId,

        // Optional — help Gemini match to the right application
        String companyHint,
        String roleHint
) {
}