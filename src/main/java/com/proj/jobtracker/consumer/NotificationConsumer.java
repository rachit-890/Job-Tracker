package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import com.proj.jobtracker.event.ReminderCreatedPayload;
import com.proj.jobtracker.event.StatusChangedPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Value("${jobtrackr.kafka.consumer-groups.notification}")
    private String consumerGroup;

    private final IdempotencyGuard idempotencyGuard;

    public NotificationConsumer(IdempotencyGuard idempotencyGuard) {
        this.idempotencyGuard = idempotencyGuard;
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
        log.info("[Notification] Status changed — applicationId={} company='{}' role='{}' {} → {}",
                payload.applicationId(), payload.company(), payload.role(),
                payload.oldStatus(), payload.newStatus());

        // TODO Phase 5: notificationService.notifyStatusChange(payload)

        ack.acknowledge();
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
        log.info("[Notification] Reminder scheduled — applicationId={} company='{}' remindAt={}",
                payload.applicationId(), payload.company(), payload.remindAt());

        // TODO Phase 5: notificationService.scheduleReminderNotification(payload)

        ack.acknowledge();
    }
}