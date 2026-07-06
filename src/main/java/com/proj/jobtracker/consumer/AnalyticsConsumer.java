package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.ApplicationCreatedPayload;
import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import com.proj.jobtracker.event.StatusChangedPayload;
import com.proj.jobtracker.service.AnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

    @Value("${jobtrackr.kafka.consumer-groups.analytics}")
    private String consumerGroup;

    private final IdempotencyGuard idempotencyGuard;
    private final AnalyticsService analyticsService;

    public AnalyticsConsumer(IdempotencyGuard idempotencyGuard,
                             AnalyticsService analyticsService) {
        this.idempotencyGuard = idempotencyGuard;
        this.analyticsService = analyticsService;
    }

    @KafkaListener(
            topics = KafkaTopics.APPLICATION_CREATED,
            groupId = "${jobtrackr.kafka.consumer-groups.analytics}"
    )
    public void onApplicationCreated(
            ConsumerRecord<String, EventEnvelope<ApplicationCreatedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<ApplicationCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        log.debug("[Analytics] ApplicationCreatedEvent: eventId={}", envelope.eventId());

        // onApplicationCreated() updates the DB snapshot AND invalidates
        // the Redis cache in a single call — no separate cache invalidation needed.
        analyticsService.onApplicationCreated();

        ack.acknowledge();
    }

    @KafkaListener(
            topics = KafkaTopics.STATUS_CHANGED,
            groupId = "${jobtrackr.kafka.consumer-groups.analytics}"
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
        log.debug("[Analytics] StatusChangedEvent: applicationId={} {} -> {}",
                payload.applicationId(), payload.oldStatus(), payload.newStatus());

        analyticsService.onStatusChanged(payload.oldStatus(), payload.newStatus());

        ack.acknowledge();
    }
}