package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import com.rachit.jobtrackr.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily job that flags stale APPLIED applications as STALE.
 *
 * "Stale" means: status is still APPLIED and appliedDate is older
 * than the configured threshold (default 14 days) with no response.
 *
 * Flow:
 * 1. Query stale candidates in pages of 100 (avoid loading all into memory)
 * 2. For each candidate, call ApplicationService.updateStatus(STALE)
 * 3. updateStatus() validates the transition, writes history, and publishes
 *    StatusChangedEvent after commit — all existing consumers react normally
 *
 * Why process via ApplicationService instead of a direct repository update?
 * A direct bulk UPDATE would bypass the status-transition domain logic and
 * the Kafka event publish. Going through ApplicationService ensures the
 * same invariants hold whether a status change comes from the API or a
 * scheduled job — no special-casing needed in consumers.
 *
 * Known limitation: if this job runs on multiple instances simultaneously,
 * the same application could be transitioned twice (the second attempt
 * will just find status=STALE and throw InvalidStatusTransitionException,
 * which is caught and logged). For true multi-instance safety, add ShedLock.
 */
@Component
public class StaleApplicationJob {

    private static final Logger log = LoggerFactory.getLogger(StaleApplicationJob.class);
    private static final int PAGE_SIZE = 100;

    private final JobApplicationRepository applicationRepository;
    private final ApplicationService applicationService;

    @Value("${jobtrackr.stale.default-threshold-days:14}")
    private int staleAfterDays;

    public StaleApplicationJob(JobApplicationRepository applicationRepository,
                               ApplicationService applicationService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
    }

    @Scheduled(cron = "${jobtrackr.scheduler.stale-cron:0 0 1 * * *}")
    public void flagStaleApplications() {
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
                    applicationService.updateStatus(application.getId(), ApplicationStatus.STALE);
                    totalFlagged++;
                    log.debug("[StaleJob] Flagged applicationId={} company={} role={}",
                            application.getId(),
                            application.getCompany(),
                            application.getRole());
                } catch (Exception e) {
                    totalFailed++;
                    log.error("[StaleJob] Failed to flag applicationId={}: {}",
                            application.getId(), e.getMessage());
                }
            }
            page++;

        } while (batch.hasNext());

        log.info("[StaleJob] Completed — flagged={} failed={}", totalFlagged, totalFailed);
    }
}