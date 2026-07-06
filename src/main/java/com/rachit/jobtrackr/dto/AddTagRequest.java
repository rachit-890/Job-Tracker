package com.rachit.jobtrackr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTagRequest(
        @NotBlank(message = "tag is required")
        @Size(min = 1, max = 100, message = "tag must be between 1 and 100 characters")
        String tag
) {
}
