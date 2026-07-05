package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import com.proj.jobtracker.event.ReminderCreatedPayload;
import com.proj.jobtracker.event.StatusChangedPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Dispatches notifications for status changes and upcoming reminders.
 *
 * Phase 3: stub — structured log output only.
 * Phase 5: real email delivery via NotificationService interface,
 *           with retry logic (15-min retry, FAILED after 3 attempts).
 *
 * Separate consumer group — subscribes to the same topics as other consumers
 * but tracks its own offset independently, so analytics or reminder failures
 * don't block notification delivery and vice versa.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String CONSUMER_GROUP = "notification-consumer-group";

    private final IdempotencyGuard idempotencyGuard;

    public NotificationConsumer(IdempotencyGuard idempotencyGuard) {
        this.idempotencyGuard = idempotencyGuard;
    }

    @KafkaListener(
            topics = KafkaTopics.STATUS_CHANGED,
            groupId = CONSUMER_GROUP
    )
    public void onStatusChanged(ConsumerRecord<String, EventEnvelope<StatusChangedPayload>> record,
                                Acknowledgment ack) {
        EventEnvelope<StatusChangedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(CONSUMER_GROUP, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        StatusChangedPayload payload = envelope.payload();
        log.info("[Notification] Status changed — applicationId={} company='{}' role='{}' {} → {}",
                payload.applicationId(), payload.company(), payload.role(),
                payload.oldStatus(), payload.newStatus());

        // TODO Phase 5: notificationService.notifyStatusChange(payload)

        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopics.REMINDER_CREATED,
            groupId = CONSUMER_GROUP
    )
    public void onReminderCreated(ConsumerRecord<String, EventEnvelope<ReminderCreatedPayload>> record,
                                  Acknowledgment ack) {
        EventEnvelope<ReminderCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(CONSUMER_GROUP, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        ReminderCreatedPayload payload = envelope.payload();
        log.info("[Notification] Reminder scheduled — applicationId={} company='{}' role='{}' remindAt={}",
                payload.applicationId(), payload.company(), payload.role(), payload.remindAt());

        // TODO Phase 5: notificationService.scheduleReminderNotification(payload)

        ack.acknowledge();
    }
}