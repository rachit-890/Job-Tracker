package com.rachit.jobtrackr.consumer;

import com.rachit.jobtrackr.event.ApplicationCreatedPayload;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.service.AiProcessingService;
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

    @Value("${jobtrackr.kafka.consumer-groups.ai}")
    private String consumerGroup;

    // FIX: declared final — constructor-injected dependencies should always
    // be final to enforce immutability and prevent accidental reassignment.
    private final IdempotencyGuard idempotencyGuard;
    private final AiProcessingService aiProcessingService;

    public AiConsumer(IdempotencyGuard idempotencyGuard,
                      AiProcessingService aiProcessingService) {
        this.idempotencyGuard = idempotencyGuard;
        this.aiProcessingService = aiProcessingService;
    }

    @KafkaListener(
            topics = KafkaTopics.APPLICATION_CREATED,
            groupId = "${jobtrackr.kafka.consumer-groups.ai}"
    )
    public void onApplicationCreated(
            ConsumerRecord<String, EventEnvelope<ApplicationCreatedPayload>> record,
            Acknowledgment ack) {

        EventEnvelope<ApplicationCreatedPayload> envelope = record.value();

        if (idempotencyGuard.alreadyProcessed(consumerGroup, envelope.eventId())) {
            ack.acknowledge();
            return;
        }

        ApplicationCreatedPayload payload = envelope.payload();
        log.info("[AI] Processing ApplicationCreatedEvent: eventId={} applicationId={} company={}",
                envelope.eventId(), payload.applicationId(), payload.company());

        aiProcessingService.processApplication(payload);

        ack.acknowledge();
        log.info("[AI] Successfully processed eventId={} consumerGroup={}",
                envelope.eventId(), consumerGroup);
    }
}