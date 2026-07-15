package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.metrics.MetricsService;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import com.rachit.jobtrackr.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StaleApplicationJob {

    private static final Logger log = LoggerFactory.getLogger(StaleApplicationJob.class);
    private static final int PAGE_SIZE = 100;

    private final JobApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final MetricsService metricsService;

    @Value("${jobtrackr.stale.default-threshold-days:14}")
    private int staleAfterDays;

    public StaleApplicationJob(JobApplicationRepository applicationRepository,
                               ApplicationService applicationService,
                               MetricsService metricsService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.metricsService = metricsService;
    }

    @Scheduled(cron = "${jobtrackr.scheduler.stale-cron:0 0 1 * * *}")
    @SchedulerLock(name = "stale-detection", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void flagStaleApplications() {
        metricsService.getStaleJobTimer().record(() -> {
            LocalDate cutoffDate = LocalDate.now().minusDays(staleAfterDays);
            log.info("[StaleJob] Starting stale detection: appliedDate <= {}", cutoffDate);

            int page = 0;
            int totalFlagged = 0;
            int totalFailed = 0;

            Page<JobApplication> batch;
            do {
                batch = applicationRepository.findStaleCandidates(
                        ApplicationStatus.APPLIED,
                        cutoffDate,
                        PageRequest.of(page, PAGE_SIZE));

                for (JobApplication application : batch.getContent()) {
                    try {
                        applicationService.updateStatus(
                                application.getId(), ApplicationStatus.STALE);
                        totalFlagged++;
                        metricsService.incrementStaleApplicationsFlagged();
                    } catch (Exception e) {
                        totalFailed++;
                        log.error("[StaleJob] Failed to flag applicationId={}: {}",
                                application.getId(), e.getMessage());
                    }
                }
                page++;
            } while (batch.hasNext());

            log.info("[StaleJob] Completed — flagged={} failed={}", totalFlagged, totalFailed);
        });
    }
}