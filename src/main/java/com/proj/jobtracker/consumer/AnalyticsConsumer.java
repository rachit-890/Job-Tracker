package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.ApplicationCreatedPayload;
import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import com.proj.jobtracker.event.StatusChangedPayload;
import com.proj.jobtracker.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Keeps the analytics_snapshot row in sync with application state changes.
 * After updating the DB, invalidates the Redis cache so the next dashboard
 * read reflects the latest data.
 *
 * Separate consumer group from AI/Reminder/Notification consumers — each
 * group independently tracks its own offset, so a failure in one consumer
 * doesn't block the others from processing the same event.
 */
@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String CONSUMER_GROUP = "analytics-consumer-group";

    private final IdempotencyGuard idempotencyGuard;
    private final AnalyticsService analyticsService;

    public AnalyticsConsumer(IdempotencyGuard idempotencyGuard,
                             AnalyticsService analyticsService) {
        this.idempotencyGuard = idempotencyGuard;
        this.analyticsService = analyticsService;
    }

    @KafkaListener(
            topics = KafkaTopics.APPLICATION_CREATED,
            groupId = CONSUMER_GROUP
    )
    public void onApplicationCreated(ConsumerRecord<String, EventEnvelope<ApplicationCreatedPayload>> record,
                                     Acknowledgment ack) {
        EventEnvelope<ApplicationCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(CONSUMER_GROUP, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        log.debug("[Analytics] Processing ApplicationCreatedEvent: eventId={}", envelope.eventId());
        analyticsService.onApplicationCreated();
        ack.acknowledge();
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
        log.debug("[Analytics] Processing StatusChangedEvent: applicationId={} {} -> {}",
                payload.applicationId(), payload.oldStatus(), payload.newStatus());

        analyticsService.onStatusChanged(payload.oldStatus(), payload.newStatus());
        ack.acknowledge();
    }
}
