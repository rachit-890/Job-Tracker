package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.entity.Reminder;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;

/**
 * Abstraction over notification delivery channels.
 *
 * Current implementation: EmailNotificationService.
 * Adding a new channel (Slack, SMS, push) means adding a new implementation
 * without touching any consumer or scheduler code.
 *
 * Methods:
 *   notifyStatusChange      — called by NotificationConsumer on StatusChangedEvent
 *   sendReminderNotification — called by ReminderDeliveryJob when remindAt arrives
 *   sendWeeklyDigest        — called by NotificationConsumer on DigestGeneratedEvent
 */
public interface NotificationService {

    void notifyStatusChange(StatusChangedPayload payload);

    void sendReminderNotification(Reminder reminder, JobApplication application);

    void sendWeeklyDigest(DigestGeneratedPayload payload);
}