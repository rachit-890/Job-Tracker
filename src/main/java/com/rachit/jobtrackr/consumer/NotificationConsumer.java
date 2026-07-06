package com.rachit.jobtrackr.consumer;

import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.event.ReminderCreatedPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
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
        log.info("[Notification] Processing StatusChangedEvent: eventId={} applicationId={} company='{}' {} → {}",
                envelope.eventId(), payload.applicationId(), payload.company(),
                payload.oldStatus(), payload.newStatus());

        // TODO Phase 5: notificationService.notifyStatusChange(payload)

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
        log.info("[Notification] Processing ReminderCreatedEvent: eventId={} applicationId={} company='{}' remindAt={}",
                envelope.eventId(), payload.applicationId(), payload.company(), payload.remindAt());

        // TODO Phase 5: notificationService.scheduleReminderNotification(payload)

        ack.acknowledge();
        log.info("[Notification] Successfully processed eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
    }
}