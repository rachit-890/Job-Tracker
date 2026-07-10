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
        log.info("[Notification] Processing StatusChangedEvent: eventId={} {} → {}",
                envelope.eventId(), payload.oldStatus(), payload.newStatus());

        notificationService.notifyStatusChange(payload);

        ack.acknowledge();
        log.info("[Notification] Successfully processed eventId={}", envelope.eventId());
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
        log.info("[Notification] Reminder scheduled: eventId={} applicationId={} company='{}' remindAt={}",
                envelope.eventId(), payload.applicationId(),
                payload.company(), payload.remindAt());

        // Actual delivery is handled by ReminderDeliveryJob when remindAt arrives.
        // This consumer only logs the scheduling confirmation.

        ack.acknowledge();
        log.info("[Notification] Successfully processed eventId={}", envelope.eventId());
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
        log.info("[Notification] Weekly digest: eventId={} week={} to {} " +
                        "newApplications={} statusChanges={} responses={}",
                envelope.eventId(), payload.weekStart(), payload.weekEnd(),
                payload.newApplications(), payload.statusChanges(),
                payload.responseCount());

        ack.acknowledge();
        log.info("[Notification] Successfully processed digest eventId={}", envelope.eventId());
    }
}