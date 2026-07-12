package com.rachit.jobtrackr.dto;

public record CompanyAnalyticsResponse(
        String company,
        int applicationCount,
        double responseRate,
        Double avgDaysToResponse
) {
}