package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.AnalyticsSummaryResponse;
import com.rachit.jobtrackr.dto.CompanyAnalyticsResponse;
import com.rachit.jobtrackr.dto.DayOfWeekCount;
import com.rachit.jobtrackr.dto.ResumePerformanceResponse;
import com.rachit.jobtrackr.dto.StatusFlowLink;
import com.rachit.jobtrackr.dto.TrendDataPoint;
import com.rachit.jobtrackr.service.AnalyticsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/summary")
    public AnalyticsSummaryResponse summary() {
        return analyticsQueryService.getSummary();
    }

    @GetMapping("/resume-performance")
    public List<ResumePerformanceResponse> resumePerformance() {
        return analyticsQueryService.getResumePerformance();
    }

    @GetMapping("/company")
    public List<CompanyAnalyticsResponse> company() {
        return analyticsQueryService.getCompanyAnalytics();
    }

    @GetMapping("/trend")
    public List<TrendDataPoint> trend(@RequestParam(defaultValue = "30d") String range) {
        return analyticsQueryService.getTrend(range);
    }

    @GetMapping("/status-flow")
    public List<StatusFlowLink> statusFlow() {
        return analyticsQueryService.getStatusFlow();
    }

    @GetMapping("/day-of-week")
    public List<DayOfWeekCount> dayOfWeek() {
        return analyticsQueryService.getDayOfWeekDistribution();
    }
}