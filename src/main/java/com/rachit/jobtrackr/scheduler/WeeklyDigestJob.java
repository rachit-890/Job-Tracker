package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.repository.ApplicationStatusHistoryRepository;
import com.rachit.jobtrackr.service.AnalyticsService;
import com.rachit.jobtrackr.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class WeeklyDigestJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestJob.class);

    private final AnalyticsService analyticsService;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final EventPublisher eventPublisher;

    public WeeklyDigestJob(AnalyticsService analyticsService,
                           ApplicationStatusHistoryRepository historyRepository,
                           EventPublisher eventPublisher) {
        this.analyticsService = analyticsService;
        this.historyRepository = historyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${jobtrackr.scheduler.digest-cron:0 0 9 * * MON}")
    public void generateWeeklyDigest() {
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minus(7, ChronoUnit.DAYS);

        // FIX: convert LocalDate to Instant at UTC midnight for index-friendly
        // range queries. toInstant is exclusive end (start of next day).
        Instant fromInstant = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = weekEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        log.info("[DigestJob] Generating weekly digest for {} to {}", weekStart, weekEnd);

        int newApplications = historyRepository.countNewApplicationsInRange(
                fromInstant, toInstant);
        int statusChanges = historyRepository.countStatusChangesInRange(
                fromInstant, toInstant);
        int responseCount = historyRepository.countResponsesInRange(
                fromInstant, toInstant);

        AnalyticsSnapshot snapshot = analyticsService.getSnapshot();
        log.info("[DigestJob] Week summary: newApplications={} statusChanges={} " +
                        "responses={} totalApplications={} responseRate={}%",
                newApplications, statusChanges, responseCount,
                snapshot.getTotalApplications(), snapshot.getResponseRate());

        eventPublisher.publishDigestGenerated(new DigestGeneratedPayload(
                weekStart,
                weekEnd,
                newApplications,
                statusChanges,
                responseCount
        ));

        log.info("[DigestJob] Completed — DigestGeneratedEvent published");
    }
}