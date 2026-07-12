package com.rachit.jobtrackr.dto;

public record TrendDataPoint(
        String date,
        int applications,
        int responses
) {
}