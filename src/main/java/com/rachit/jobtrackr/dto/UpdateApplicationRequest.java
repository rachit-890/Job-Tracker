package com.rachit.jobtrackr.dto;

public record UpdateApplicationRequest(
        String company,
        String role,
        String jdText,
        String resumeVersion,
        String sourceUrl
) {
}
