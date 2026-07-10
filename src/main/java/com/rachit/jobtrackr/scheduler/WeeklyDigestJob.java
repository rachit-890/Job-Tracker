package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.repository.AnalyticsSnapshotRepository;
import com.rachit.jobtrackr.repository.ApplicationStatusHistoryRepository;
import com.rachit.jobtrackr.service.AnalyticsService;
import com.rachit.jobtrackr.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Weekly job that summarizes the past week's job search activity
 * and publishes a DigestGeneratedEvent.
 *
 * Runs every Monday at 9am (configurable via jobtrackr.scheduler.digest-cron).
 *
 * The DigestGeneratedEvent payload carries counts that the NotificationConsumer
 * can use to send a weekly email summary — "You applied to 5 jobs this week,
 * got 2 responses, and your response rate is now 12%."
 */
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

        log.info("[DigestJob] Generating weekly digest for {} to {}", weekStart, weekEnd);

        // Count new applications this week
        int newApplications = historyRepository
                .countNewApplicationsInRange(weekStart, weekEnd);

        // Count status changes this week (responses = any move past APPLIED)
        int statusChanges = historyRepository
                .countStatusChangesInRange(weekStart, weekEnd);

        // Count responses (transitions away from APPLIED — screening/interview/rejected)
        int responseCount = historyRepository
                .countResponsesInRange(weekStart, weekEnd);

        AnalyticsSnapshot snapshot = analyticsService.getSnapshot();
        log.info("[DigestJob] Week summary: newApplications={} statusChanges={} responses={} " +
                        "totalApplications={} responseRate={}%",
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