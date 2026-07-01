package com.proj.jobtracker.dto;

public record UpdateApplicationRequest(
        String company,
        String role,
        String jdText,
        String resumeVersion,
        String sourceUrl
) {
}
