package com.rachit.jobtrackr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachit.jobtrackr.entity.OutboxEvent;
import com.rachit.jobtrackr.event.ApplicationCreatedPayload;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.event.ReminderCreatedPayload;
import com.rachit.jobtrackr.event.ResumeScoredPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
import com.rachit.jobtrackr.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Single entry point for all domain event publishes.
 *
 * OUTBOX PATTERN (Phase 1 upgrade):
 * Previously, this class sent events directly to Kafka using KafkaTemplate
 * inside a publishAfterCommit hook. This left a window where a crash between
 * DB commit and Kafka send would lose the event.
 *
 * Now, publish methods write an OutboxEvent row to the outbox_events table
 * within the caller's existing transaction. The OutboxPollerJob reads
 * unpublished rows and sends them to Kafka, then marks them published.
 *
 * This guarantees at-least-once delivery: if the transaction commits,
 * the event WILL be published. Consumers already have IdempotencyGuard
 * to handle duplicates.
 *
 * Message key strategy is preserved in the aggregateId field:
 * - ApplicationCreatedEvent  → applicationId as key
 * - StatusChangedEvent       → applicationId as key
 * - ResumeScoredEvent        → applicationId as key
 * - ReminderCreatedEvent     → applicationId as key
 * - DigestGeneratedEvent     → fixed key "digest"
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public EventPublisher(OutboxRepository outboxRepository,
                          ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void publishApplicationCreated(ApplicationCreatedPayload payload) {
        EventEnvelope<ApplicationCreatedPayload> envelope =
                EventEnvelope.of("ApplicationCreatedEvent", payload);
        writeToOutbox(
                KafkaTopics.APPLICATION_CREATED,
                payload.applicationId().toString(),
                "ApplicationCreatedEvent",
                envelope);
    }

    public void publishStatusChanged(StatusChangedPayload payload) {
        EventEnvelope<StatusChangedPayload> envelope =
                EventEnvelope.of("StatusChangedEvent", payload);
        writeToOutbox(
                KafkaTopics.STATUS_CHANGED,
                payload.applicationId().toString(),
                "StatusChangedEvent",
                envelope);
    }

    public void publishResumeScored(ResumeScoredPayload payload) {
        EventEnvelope<ResumeScoredPayload> envelope =
                EventEnvelope.of("ResumeScoredEvent", payload);
        writeToOutbox(
                KafkaTopics.RESUME_SCORED,
                payload.applicationId().toString(),
                "ResumeScoredEvent",
                envelope);
    }

    public void publishReminderCreated(ReminderCreatedPayload payload) {
        EventEnvelope<ReminderCreatedPayload> envelope =
                EventEnvelope.of("ReminderCreatedEvent", payload);
        writeToOutbox(
                KafkaTopics.REMINDER_CREATED,
                payload.applicationId().toString(),
                "ReminderCreatedEvent",
                envelope);
    }

    public void publishDigestGenerated(DigestGeneratedPayload payload) {
        EventEnvelope<DigestGeneratedPayload> envelope =
                EventEnvelope.of("DigestGeneratedEvent", payload);
        writeToOutbox(
                KafkaTopics.DIGEST_GENERATED,
                "digest",
                "DigestGeneratedEvent",
                envelope);
    }

    /**
     * Writes an event to the outbox table within the caller's transaction.
     * The OutboxPollerJob will pick it up and send it to Kafka.
     */
    private void writeToOutbox(String topic, String aggregateId,
                               String eventType, Object envelope) {
        try {
            String payloadJson = objectMapper.writeValueAsString(envelope);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payloadJson)
                    .published(false)
                    .build();

            outboxRepository.save(outboxEvent);
            log.debug("Wrote outbox event: type={} aggregateId={} topic={}",
                    eventType, aggregateId, topic);
        } catch (Exception e) {
            log.error("Failed to write outbox event: type={} aggregateId={}",
                    eventType, aggregateId, e);
            throw new RuntimeException("Failed to serialize event for outbox", e);
        }
    }
}