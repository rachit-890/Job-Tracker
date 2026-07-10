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
import java.util.Optional;

/**
 * Polls for due reminders every 15 minutes and attempts delivery.
 *
 * FIX: now processes ALL pages of due reminders, not just the first one.
 * Previously PageRequest.of(0, PAGE_SIZE) was called on every iteration,
 * meaning pages 1, 2, 3... were never reached if more than PAGE_SIZE
 * reminders were due simultaneously.
 *
 * Why reset to page 0 on each loop iteration instead of incrementing?
 * After processing a page, those reminders are updated to SENT/FAILED/rescheduled.
 * They no longer appear in subsequent queries for PENDING reminders due now.
 * So we always query page 0 — the result set shrinks with each iteration
 * until it's empty. Incrementing the page number would skip records.
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

        int totalDelivered = 0;
        int totalFailed = 0;
        int totalCancelled = 0;

        // FIX: always query page 0 — each iteration updates processed reminders
        // out of PENDING status, so page 0 always contains the next unprocessed batch.
        // Incrementing the page number would skip reminders that shifted positions
        // as earlier ones were processed.
        Page<Reminder> page;
        do {
            page = reminderRepository.findByRemindAtBeforeAndStatus(
                    now, ReminderStatus.PENDING, PageRequest.of(0, PAGE_SIZE));

            if (page.isEmpty()) break;

            log.info("[ReminderJob] Processing batch of {} due reminder(s)",
                    page.getNumberOfElements());

            for (Reminder reminder : page.getContent()) {
                ReminderResult result = processReminder(reminder);
                switch (result) {
                    case DELIVERED -> totalDelivered++;
                    case FAILED    -> totalFailed++;
                    case CANCELLED -> totalCancelled++;
                }
            }

        } while (page.hasNext());

        if (totalDelivered + totalFailed + totalCancelled > 0) {
            log.info("[ReminderJob] Completed — delivered={} failed={} cancelled={}",
                    totalDelivered, totalFailed, totalCancelled);
        } else {
            log.debug("[ReminderJob] No due reminders");
        }
    }

    @Transactional
    protected ReminderResult processReminder(Reminder reminder) {
        Optional<JobApplication> applicationOpt =
                applicationRepository.findByIdAndDeletedFalse(reminder.getApplicationId());

        if (applicationOpt.isEmpty()) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRepository.save(reminder);
            log.warn("[ReminderJob] Application not found for reminderId={} — cancelled",
                    reminder.getId());
            return ReminderResult.CANCELLED;
        }

        JobApplication application = applicationOpt.get();

        try {
            notificationService.sendReminderNotification(reminder, application);
            reminder.setStatus(ReminderStatus.SENT);
            reminderRepository.save(reminder);
            log.info("[ReminderJob] Reminder delivered: reminderId={} applicationId={} company={}",
                    reminder.getId(), application.getId(), application.getCompany());
            return ReminderResult.DELIVERED;

        } catch (Exception e) {
            int newAttemptCount = reminder.getAttemptCount() + 1;
            reminder.setAttemptCount(newAttemptCount);

            if (newAttemptCount >= maxAttempts) {
                reminder.setStatus(ReminderStatus.FAILED);
                reminderRepository.save(reminder);
                log.error("[ReminderJob] Reminder FAILED after {} attempts: reminderId={} applicationId={}",
                        maxAttempts, reminder.getId(), application.getId(), e);
            } else {
                reminder.setRemindAt(Instant.now().plusMillis(retryDelayMs));
                reminderRepository.save(reminder);
                log.warn("[ReminderJob] Delivery failed (attempt {}/{}), rescheduled in {}min: reminderId={}",
                        newAttemptCount, maxAttempts,
                        retryDelayMs / 60000, reminder.getId());
            }
            return ReminderResult.FAILED;
        }
    }

    private enum ReminderResult {
        DELIVERED, FAILED, CANCELLED
    }
}