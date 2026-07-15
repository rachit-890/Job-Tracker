package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResumeVersionRequest(
        @NotBlank(message = "Label is required")
        @Size(max = 100, message = "Label must be 100 characters or fewer")
        String label,
        
        @NotBlank(message = "Content is required")
        @Size(max = 20000, message = "Content must be 20000 characters or fewer")
        String content
) {}
