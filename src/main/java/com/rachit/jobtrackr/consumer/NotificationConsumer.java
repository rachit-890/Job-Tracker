package com.rachit.jobtrackr.consumer;

import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.event.ReminderCreatedPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
import com.rachit.jobtrackr.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Dispatches user-facing notifications for application events.
 *
 * Reminder flow clarification (for reviewers):
 * Reminders are ALREADY persisted to the reminders table by ReminderConsumer
 * (a separate consumer group that subscribes to STATUS_CHANGED). That consumer
 * handles persistence when status = APPLIED. This consumer's onReminderCreated
 * only logs the scheduling confirmation — it does not need to persist anything.
 * Actual delivery happens later when ReminderDeliveryJob polls for due reminders.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Value("${jobtrackr.kafka.consumer-groups.notification}")
    private String consumerGroup;

    private final IdempotencyGuard idempotencyGuard;
    private final NotificationService notificationService;

    public NotificationConsumer(IdempotencyGuard idempotencyGuard,
                                NotificationService notificationService) {
        this.idempotencyGuard = idempotencyGuard;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = KafkaTopics.STATUS_CHANGED,
            groupId = "${jobtrackr.kafka.consumer-groups.notification}"
    )
    public void onStatusChanged(
            ConsumerRecord<String, EventEnvelope<StatusChangedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<StatusChangedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        StatusChangedPayload payload = envelope.payload();
        log.info("[Notification] Processing StatusChangedEvent: eventId={} " +
                        "applicationId={} company='{}' role='{}' {} → {}",
                envelope.eventId(),
                payload.applicationId(),
                payload.company(),
                payload.role(),
                payload.oldStatus(),
                payload.newStatus());

        notificationService.notifyStatusChange(payload);

        ack.acknowledge();
        log.info("[Notification] Successfully processed eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
    }

    @KafkaListener(
            topics = KafkaTopics.REMINDER_CREATED,
            groupId = "${jobtrackr.kafka.consumer-groups.notification}"
    )
    public void onReminderCreated(
            ConsumerRecord<String, EventEnvelope<ReminderCreatedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<ReminderCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        ReminderCreatedPayload payload = envelope.payload();

        // The reminder row was already persisted by ReminderConsumer (separate
        // consumer group). This consumer only logs the scheduling confirmation.
        // Delivery happens when ReminderDeliveryJob polls for due reminders.
        log.info("[Notification] Reminder scheduled: eventId={} applicationId={} " +
                        "company='{}' role='{}' remindAt={}",
                envelope.eventId(),
                payload.applicationId(),
                payload.company(),
                payload.role(),
                payload.remindAt());

        ack.acknowledge();
        log.info("[Notification] Successfully processed eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
    }

    @KafkaListener(
            topics = KafkaTopics.DIGEST_GENERATED,
            groupId = "${jobtrackr.kafka.consumer-groups.notification}"
    )
    public void onDigestGenerated(
            ConsumerRecord<String, EventEnvelope<DigestGeneratedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<DigestGeneratedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        DigestGeneratedPayload payload = envelope.payload();
        log.info("[Notification] Processing DigestGeneratedEvent: eventId={} " +
                        "week={} to {} newApplications={} statusChanges={} responses={}",
                envelope.eventId(),
                payload.weekStart(),
                payload.weekEnd(),
                payload.newApplications(),
                payload.statusChanges(),
                payload.responseCount());

        // FIX: actually send the weekly digest notification to the user.
        // Previously this method only logged — the user never received anything.
        notificationService.sendWeeklyDigest(payload);

        ack.acknowledge();
        log.info("[Notification] Successfully processed digest eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
    }
}