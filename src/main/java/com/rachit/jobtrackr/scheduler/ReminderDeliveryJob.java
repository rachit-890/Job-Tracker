package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.domain.ReminderStatus;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.entity.Reminder;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import com.rachit.jobtrackr.repository.ReminderRepository;
import com.rachit.jobtrackr.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Polls for due reminders every 15 minutes (configurable) and attempts delivery.
 *
 * Retry logic:
 * - On success      → status = SENT
 * - On failure, attemptCount < maxAttempts → reschedule 15 min out, increment count
 * - On failure, attemptCount >= maxAttempts → status = FAILED (no more retries)
 *
 * Why poll instead of pure event-driven delivery?
 * Kafka's ReminderCreatedEvent fires immediately when a reminder is scheduled,
 * but we need to deliver it at a future point in time (N days later). Polling
 * the DB on a short interval is the standard approach for time-based delivery —
 * it's simple, reliable, and doesn't require an external scheduler service.
 *
 * Why process in pages?
 * Avoid loading all due reminders into memory if a backlog accumulates
 * (e.g. after a service restart when reminders piled up).
 */
@Component
public class ReminderDeliveryJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderDeliveryJob.class);
    private static final int PAGE_SIZE = 50;

    private final ReminderRepository reminderRepository;
    private final JobApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    @Value("${jobtrackr.scheduler.reminder-max-attempts:3}")
    private int maxAttempts;

    @Value("${jobtrackr.scheduler.reminder-delay-ms:900000}")
    private long retryDelayMs;

    public ReminderDeliveryJob(ReminderRepository reminderRepository,
                               JobApplicationRepository applicationRepository,
                               NotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${jobtrackr.scheduler.reminder-delay-ms:900000}")
    public void deliverDueReminders() {
        Instant now = Instant.now();
        log.debug("[ReminderJob] Checking for due reminders at {}", now);

        Page<Reminder> duePage = reminderRepository.findByRemindAtBeforeAndStatus(
                now, ReminderStatus.PENDING, PageRequest.of(0, PAGE_SIZE));

        if (duePage.isEmpty()) {
            log.debug("[ReminderJob] No due reminders");
            return;
        }

        log.info("[ReminderJob] Found {} due reminder(s)", duePage.getTotalElements());

        for (Reminder reminder : duePage.getContent()) {
            processReminder(reminder);
        }
    }

    @Transactional
    protected void processReminder(Reminder reminder) {
        Optional<JobApplication> applicationOpt =
                applicationRepository.findByIdAndDeletedFalse(reminder.getApplicationId());

        if (applicationOpt.isEmpty()) {
            // Application was deleted — cancel the reminder silently
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRepository.save(reminder);
            log.warn("[ReminderJob] Application not found for reminderId={} — cancelled",
                    reminder.getId());
            return;
        }

        JobApplication application = applicationOpt.get();

        try {
            notificationService.sendReminderNotification(reminder, application);
            reminder.setStatus(ReminderStatus.SENT);
            reminderRepository.save(reminder);
            log.info("[ReminderJob] Reminder delivered: reminderId={} applicationId={} company={}",
                    reminder.getId(), application.getId(), application.getCompany());

        } catch (Exception e) {
            int newAttemptCount = reminder.getAttemptCount() + 1;
            reminder.setAttemptCount(newAttemptCount);

            if (newAttemptCount >= maxAttempts) {
                reminder.setStatus(ReminderStatus.FAILED);
                log.error("[ReminderJob] Reminder FAILED after {} attempts: reminderId={} applicationId={}",
                        maxAttempts, reminder.getId(), application.getId(), e);
            } else {
                // Reschedule for retryDelayMs from now (default 15 minutes)
                reminder.setRemindAt(Instant.now().plusMillis(retryDelayMs));
                log.warn("[ReminderJob] Reminder delivery failed (attempt {}/{}), " +
                                "rescheduled in {}min: reminderId={}",
                        newAttemptCount, maxAttempts,
                        retryDelayMs / 60000, reminder.getId(), e);
            }

            reminderRepository.save(reminder);
        }
    }
}