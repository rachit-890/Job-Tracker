package com.rachit.jobtrackr.dto;

/**
 * Number of applications submitted on a given day of the week.
 * Used by the day-of-week heatmap visualization.
 */
public record DayOfWeekCount(
        String day,
        long count
) {
}
