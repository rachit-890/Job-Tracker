package com.rachit.jobtrackr.dto;

public record ResumePerformanceResponse(
        String resumeVersion,
        long totalApplications,    // FIX: count() returns Long not int
        double callbackRate
) {
}