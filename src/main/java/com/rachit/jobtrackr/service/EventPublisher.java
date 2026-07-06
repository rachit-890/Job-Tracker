package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.event.ApplicationCreatedPayload;
import com.rachit.jobtrackr.event.DigestGeneratedPayload;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.event.ReminderCreatedPayload;
import com.rachit.jobtrackr.event.ResumeScoredPayload;
import com.rachit.jobtrackr.event.StatusChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Single entry point for all Kafka publishes.
 *
 * Message key strategy:
 * - ApplicationCreatedEvent  → applicationId as key
 *   (all events for the same application go to the same partition)
 * - StatusChangedEvent       → applicationId as key
 *   (ordering per application is critical — APPLIED → SCREENING → INTERVIEW
 *    must arrive in order; random keys would scatter them across partitions)
 * - ResumeScoredEvent        → applicationId as key
 * - ReminderCreatedEvent     → applicationId as key
 * - DigestGeneratedEvent     → fixed key "digest" (singleton event type)
 *
 * Why does key = applicationId matter for ordering?
 * Kafka guarantees ordering only within a single partition.
 * Two messages with the same key always go to the same partition.
 * If we used random keys (or eventId), two status changes for the same
 * application could land on different partitions and be consumed out of order,
 * producing an inconsistent state (e.g. REJECTED processed before SCREENING).
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishApplicationCreated(ApplicationCreatedPayload payload) {
        EventEnvelope<ApplicationCreatedPayload> envelope =
                EventEnvelope.of("ApplicationCreatedEvent", payload);
        // Key = applicationId — all events for this application go to same partition
        send(KafkaTopics.APPLICATION_CREATED, payload.applicationId().toString(), envelope);
    }

    public void publishStatusChanged(StatusChangedPayload payload) {
        EventEnvelope<StatusChangedPayload> envelope =
                EventEnvelope.of("StatusChangedEvent", payload);
        // Key = applicationId — ordering of status changes per application is critical
        send(KafkaTopics.STATUS_CHANGED, payload.applicationId().toString(), envelope);
    }

    public void publishResumeScored(ResumeScoredPayload payload) {
        EventEnvelope<ResumeScoredPayload> envelope =
                EventEnvelope.of("ResumeScoredEvent", payload);
        send(KafkaTopics.RESUME_SCORED, payload.applicationId().toString(), envelope);
    }

    public void publishReminderCreated(ReminderCreatedPayload payload) {
        EventEnvelope<ReminderCreatedPayload> envelope =
                EventEnvelope.of("ReminderCreatedEvent", payload);
        send(KafkaTopics.REMINDER_CREATED, payload.applicationId().toString(), envelope);
    }

    public void publishDigestGenerated(DigestGeneratedPayload payload) {
        EventEnvelope<DigestGeneratedPayload> envelope =
                EventEnvelope.of("DigestGeneratedEvent", payload);
        // Digest is not per-application so we use a fixed key
        send(KafkaTopics.DIGEST_GENERATED, "digest", envelope);
    }

    private void send(String topic, String key, Object envelope) {
        kafkaTemplate.send(topic, key, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic={} key={}",
                                topic, key, ex);
                    } else {
                        log.debug("Published event to topic={} key={} partition={} offset={}",
                                topic,
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}