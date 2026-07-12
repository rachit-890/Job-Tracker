package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.dto.AnalyticsSummaryResponse;
import com.rachit.jobtrackr.dto.CompanyAnalyticsResponse;
import com.rachit.jobtrackr.dto.ResumePerformanceResponse;
import com.rachit.jobtrackr.dto.TrendDataPoint;
import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serves dashboard read queries.
 * Summary comes from the pre-aggregated analytics_snapshot (maintained by
 * AnalyticsConsumer) and is cached in Redis for 60 seconds.
 * Resume performance, company analytics, and trend are computed from raw
 * application rows via JPQL aggregate queries.
 */
@Service
public class AnalyticsQueryService {

    private static final String CACHE_KEY = AnalyticsService.ANALYTICS_CACHE_KEY;
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final AnalyticsService analyticsService;
    private final JobApplicationRepository applicationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AnalyticsQueryService(AnalyticsService analyticsService,
                                 JobApplicationRepository applicationRepository,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.analyticsService = analyticsService;
        this.applicationRepository = applicationRepository;
        this.redisTemplate = redisTemplate;
    }

    public AnalyticsSummaryResponse getSummary() {
        AnalyticsSnapshot snapshot = analyticsService.getSnapshot();

        Map<String, Integer> breakdown = Map.of(
                "APPLIED",    snapshot.getAppliedCount(),
                "SCREENING",  snapshot.getScreeningCount(),
                "INTERVIEW",  snapshot.getInterviewCount(),
                "OFFER",      snapshot.getOfferCount(),
                "REJECTED",   snapshot.getRejectedCount(),
                "STALE",      snapshot.getStaleCount()
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
        return applicationRepository.computeResumePerformance();
    }

    public List<CompanyAnalyticsResponse> getCompanyAnalytics() {
        return applicationRepository.computeCompanyAnalytics();
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