package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.entity.Reminder;
import com.rachit.jobtrackr.event.StatusChangedPayload;

/**
 * Abstraction over notification delivery channels.
 *
 * Why an interface here?
 * Consumers and schedulers call NotificationService — they don't care
 * whether delivery happens via email, Slack, SMS, or a log statement.
 * Adding a new channel means adding a new implementation, not touching
 * the consumers. This is the extensibility point mentioned in the spec.
 *
 * Current implementation: EmailNotificationService.
 * Phase 5 ships with email support (or log-stub if SMTP not configured).
 * Future: SlackNotificationService, SmsNotificationService, etc.
 */
public interface NotificationService {

    /**
     * Notifies that an application's status changed.
     * Called by NotificationConsumer on every StatusChangedEvent.
     */
    void notifyStatusChange(StatusChangedPayload payload);

    /**
     * Sends a follow-up reminder for an application with no response.
     * Called by ReminderDeliveryJob when a reminder's remindAt time has arrived.
     */
    void sendReminderNotification(Reminder reminder, JobApplication application);
}