package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.dto.AnalyticsSummaryResponse;
import com.rachit.jobtrackr.dto.CompanyAnalyticsResponse;
import com.rachit.jobtrackr.dto.ResumePerformanceResponse;
import com.rachit.jobtrackr.dto.TrendDataPoint;
import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsQueryService {

    private final AnalyticsService analyticsService;
    private final JobApplicationRepository applicationRepository;

    public AnalyticsQueryService(AnalyticsService analyticsService,
                                 JobApplicationRepository applicationRepository) {
        this.analyticsService = analyticsService;
        this.applicationRepository = applicationRepository;
    }

    public AnalyticsSummaryResponse getSummary() {
        AnalyticsSnapshot snapshot = analyticsService.getSnapshot();

        Map<String, Integer> breakdown = Map.of(
                "APPLIED",   snapshot.getAppliedCount(),
                "SCREENING", snapshot.getScreeningCount(),
                "INTERVIEW", snapshot.getInterviewCount(),
                "OFFER",     snapshot.getOfferCount(),
                "REJECTED",  snapshot.getRejectedCount(),
                "STALE",     snapshot.getStaleCount()
        );

        return new AnalyticsSummaryResponse(
                snapshot.getTotalApplications(),
                snapshot.getResponseRate(),
                applicationRepository.computeAvgTimeToResponse(),
                snapshot.getAppliedCount(),
                snapshot.getScreeningCount(),
                snapshot.getInterviewCount(),
                snapshot.getOfferCount(),
                snapshot.getRejectedCount(),
                snapshot.getStaleCount(),
                breakdown
        );
    }

    public List<ResumePerformanceResponse> getResumePerformance() {
        List<Object[]> rows = applicationRepository.computeResumePerformanceRaw();
        return rows.stream().map(row -> {
            String version = (String) row[0];
            long total = ((Number) row[1]).longValue();
            long callbacks = ((Number) row[2]).longValue();
            double rate = total == 0 ? 0.0
                    : Math.round((callbacks * 100.0 / total) * 100.0) / 100.0;
            return new ResumePerformanceResponse(version, total, rate);
        }).toList();
    }

    public List<CompanyAnalyticsResponse> getCompanyAnalytics() {
        List<Object[]> rows = applicationRepository.computeCompanyAnalyticsRaw();
        return rows.stream().map(row -> {
            String company = (String) row[0];
            long total = ((Number) row[1]).longValue();
            long callbacks = ((Number) row[2]).longValue();
            double rate = total == 0 ? 0.0
                    : Math.round((callbacks * 100.0 / total) * 100.0) / 100.0;
            return new CompanyAnalyticsResponse(company, total, rate, null);
        }).toList();
    }

    public List<TrendDataPoint> getTrend(String range) {
        int days = switch (range) {
            case "7d"  -> 7;
            case "90d" -> 90;
            default    -> 30;
        };

        List<TrendDataPoint> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            int applications = applicationRepository.countCreatedInRange(from, to);
            int responses    = applicationRepository.countResponsesInRange(from, to);

            result.add(new TrendDataPoint(date.format(fmt), applications, responses));
        }

        return result;
    }
}