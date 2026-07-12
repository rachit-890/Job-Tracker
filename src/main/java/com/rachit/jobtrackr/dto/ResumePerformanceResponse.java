package com.rachit.jobtrackr.dto;

public record ResumePerformanceResponse(
        String resumeVersion,
        int totalApplications,
        double callbackRate
) {
}