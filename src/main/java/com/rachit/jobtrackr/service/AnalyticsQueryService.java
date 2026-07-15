package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.AnalyticsSummaryResponse;
import com.rachit.jobtrackr.dto.CompanyAnalyticsResponse;
import com.rachit.jobtrackr.dto.DayOfWeekCount;
import com.rachit.jobtrackr.dto.ResumePerformanceResponse;
import com.rachit.jobtrackr.dto.StatusFlowLink;
import com.rachit.jobtrackr.dto.TrendDataPoint;
import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.repository.ApplicationStatusHistoryRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsQueryService {

    private static final String[] DAY_NAMES = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private final AnalyticsService analyticsService;
    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;

    public AnalyticsQueryService(AnalyticsService analyticsService,
                                 JobApplicationRepository applicationRepository,
                                 ApplicationStatusHistoryRepository historyRepository) {
        this.analyticsService = analyticsService;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
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

    /**
     * Returns status transition links for the Sankey flow diagram.
     * Each link represents a (fromStatus → toStatus) edge with the count
     * of applications that made that transition.
     */
    public List<StatusFlowLink> getStatusFlow() {
        List<Object[]> rows = historyRepository.countTransitionsByStatusPair();
        return rows.stream().map(row -> {
            ApplicationStatus oldStatus = (ApplicationStatus) row[0];
            ApplicationStatus newStatus = (ApplicationStatus) row[1];
            long count = ((Number) row[2]).longValue();
            return new StatusFlowLink(oldStatus.name(), newStatus.name(), count);
        }).toList();
    }

    /**
     * Returns application counts by day of week for the heatmap.
     * Always returns all 7 days (Mon-Sun), filling zeros for days with no applications.
     * Order: Monday first (business week convention).
     */
    public List<DayOfWeekCount> getDayOfWeekDistribution() {
        List<Object[]> rows = applicationRepository.countByDayOfWeek();

        // Build a lookup: PostgreSQL DOW (0=Sun..6=Sat) → count
        Map<Integer, Long> dowMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int dow = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            dowMap.put(dow, count);
        }

        // Return Monday-first order (DOW: 1,2,3,4,5,6,0)
        List<DayOfWeekCount> result = new ArrayList<>();
        int[] mondayFirst = {1, 2, 3, 4, 5, 6, 0};
        for (int dow : mondayFirst) {
            result.add(new DayOfWeekCount(DAY_NAMES[dow], dowMap.getOrDefault(dow, 0L)));
        }
        return result;
    }
}