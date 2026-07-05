package com.proj.jobtracker.consumer;

import com.proj.jobtracker.event.ApplicationCreatedPayload;
import com.proj.jobtracker.event.EventEnvelope;
import com.proj.jobtracker.event.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes ApplicationCreatedEvent and triggers AI processing.
 *
 * Phase 3: stub — marks the event idempotent and logs it.
 * Phase 4: fills in Gemini JD tag extraction and resume embedding calls.
 *
 * Why a separate consumer for AI work?
 * If AI calls were synchronous in the create endpoint, a Gemini timeout
 * would make every application creation slow or fail. Here, AI processing
 * is fully decoupled: the API returns 201 immediately, the consumer handles
 * Gemini asynchronously with its own retry + DLT safety net.
 */
@Component
public class AiConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiConsumer.class);
    private static final String CONSUMER_GROUP = "ai-consumer-group";

    private final IdempotencyGuard idempotencyGuard;

    public AiConsumer(IdempotencyGuard idempotencyGuard) {
        this.idempotencyGuard = idempotencyGuard;
    }

    @KafkaListener(
            topics = KafkaTopics.APPLICATION_CREATED,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationCreated(ConsumerRecord<String, EventEnvelope<ApplicationCreatedPayload>> record,
                                     Acknowledgment ack) {
        EventEnvelope<ApplicationCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(CONSUMER_GROUP, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        ApplicationCreatedPayload payload = envelope.payload();
        log.info("[AI Consumer] Received ApplicationCreatedEvent: applicationId={} company={} role={}",
                payload.applicationId(), payload.company(), payload.role());

        // TODO Phase 4: call Gemini here —
        //   1. Extract JD tags from payload.jdText() and save to application_tags
        //   2. Compute resume embedding for payload.resumeVersion() (check cache first)
        //   3. Compute JD embedding
        //   4. Calculate cosine similarity → update applications.match_score
        //   5. Publish ResumeScoredEvent

        ack.acknowledge();
        log.debug("[AI Consumer] Acknowledged event: eventId={}", envelope.eventId());
    }
}
