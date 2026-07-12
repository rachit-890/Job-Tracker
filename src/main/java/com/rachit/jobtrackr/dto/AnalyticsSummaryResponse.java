package com.rachit.jobtrackr.dto;

import java.util.Map;

public record AnalyticsSummaryResponse(
        int totalApplications,
        double responseRate,
        Double avgTimeToResponse,
        int appliedCount,
        int screeningCount,
        int interviewCount,
        int offerCount,
        int rejectedCount,
        int staleCount,
        Map<String, Integer> statusBreakdown
) {
}