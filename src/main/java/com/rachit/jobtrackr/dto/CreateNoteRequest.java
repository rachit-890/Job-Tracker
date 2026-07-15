package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank(message = "Note content is required")
        @Size(max = 5000, message = "Note must be 5000 characters or fewer")
        String content
) {}
