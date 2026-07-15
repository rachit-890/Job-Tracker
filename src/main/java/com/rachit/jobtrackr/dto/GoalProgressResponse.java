package com.rachit.jobtrackr.dto;

import com.rachit.jobtrackr.domain.GoalPeriod;

public record GoalProgressResponse(
        Integer targetCount,
        GoalPeriod period,
        long currentCount,
        int progressPercentage
) {}
