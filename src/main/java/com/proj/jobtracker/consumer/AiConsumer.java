package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.ApplicationCreatedPayload;
import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class AiConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiConsumer.class);

    // FIX: injected from application.yml instead of hardcoded string constant.
    @Value("${jobtrackr.kafka.consumer-groups.ai}")
    private String consumerGroup;

    private final IdempotencyGuard idempotencyGuard;

    public AiConsumer(IdempotencyGuard idempotencyGuard) {
        this.idempotencyGuard = idempotencyGuard;
    }

    @KafkaListener(
            topics = KafkaTopics.APPLICATION_CREATED,
            groupId = "${jobtrackr.kafka.consumer-groups.ai}"
    )
    public void onApplicationCreated(
            ConsumerRecord<String, EventEnvelope<ApplicationCreatedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<ApplicationCreatedPayload> envelope = record.value();

        // alreadyProcessed() saves the (consumerGroup, eventId) record to DB
        // in a REQUIRES_NEW transaction BEFORE returning false.
        // So by the time ack.acknowledge() is called below, the idempotency
        // record is already committed — order is correct.
        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        ApplicationCreatedPayload payload = envelope.payload();
        log.info("[AI] ApplicationCreatedEvent: applicationId={} company={} role={}",
                payload.applicationId(), payload.company(), payload.role());

        // TODO Phase 4: Gemini JD tag extraction + resume embedding + match score

        ack.acknowledge();
        log.debug("[AI] Acknowledged eventId={}", envelope.eventId());
    }
}