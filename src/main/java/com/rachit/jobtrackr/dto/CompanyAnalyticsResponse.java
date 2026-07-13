package com.rachit.jobtrackr.dto;

public record CompanyAnalyticsResponse(
        String company,
        long applicationCount,     // FIX: count() returns Long not int
        double responseRate,
        Double avgDaysToResponse
) {
}