package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.event.*;
import com.rachit.jobtrackr.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Single entry point for all Kafka publishes.
 * Services never touch KafkaTemplate directly — they call this class.
 * This makes it easy to stub in tests and keeps topic names out of
 * business logic code.
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
        send(KafkaTopics.APPLICATION_CREATED, envelope.eventId().toString(), envelope);
    }

    public void publishStatusChanged(StatusChangedPayload payload) {
        EventEnvelope<StatusChangedPayload> envelope =
                EventEnvelope.of("StatusChangedEvent", payload);
        send(KafkaTopics.STATUS_CHANGED, envelope.eventId().toString(), envelope);
    }

    public void publishResumeScored(ResumeScoredPayload payload) {
        EventEnvelope<ResumeScoredPayload> envelope =
                EventEnvelope.of("ResumeScoredEvent", payload);
        send(KafkaTopics.RESUME_SCORED, envelope.eventId().toString(), envelope);
    }

    public void publishReminderCreated(ReminderCreatedPayload payload) {
        EventEnvelope<ReminderCreatedPayload> envelope =
                EventEnvelope.of("ReminderCreatedEvent", payload);
        send(KafkaTopics.REMINDER_CREATED, envelope.eventId().toString(), envelope);
    }

    public void publishDigestGenerated(DigestGeneratedPayload payload) {
        EventEnvelope<DigestGeneratedPayload> envelope =
                EventEnvelope.of("DigestGeneratedEvent", payload);
        send(KafkaTopics.DIGEST_GENERATED, envelope.eventId().toString(), envelope);
    }

    private void send(String topic, String key, Object envelope) {
        kafkaTemplate.send(topic, key, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to topic={} key={}", topic, key, ex);
                    } else {
                        log.debug("Published event to topic={} partition={} offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}