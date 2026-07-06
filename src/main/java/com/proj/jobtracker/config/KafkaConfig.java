package com.proj.jobtracker.config;

import com.proj.jobtracker.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Creates all Kafka topics explicitly.
 * AUTO_CREATE is disabled in docker-compose so we own the topic lifecycle.
 * DLT strategy:
 * Every main topic has a corresponding DLT defined as a NewTopic bean.
 * The DeadLetterPublishingRecoverer routes to these explicit DLT names.
 * All DLTs use the convention: <original-topic>.DLT
 * Why explicit beans for ALL DLTs?
 * docker-compose sets KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=false.
 * If a consumer fails and tries to publish to a DLT that doesn't exist,
 * the producer itself will fail — turning a processing error into an
 * unrecoverable producer error. Every DLT must be pre-created.
 */
@Configuration
public class KafkaConfig {

    // ── Main topics ──────────────────────────────────────────────────────────

    @Bean
    public NewTopic applicationCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_CREATED)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic statusChangedTopic() {
        return TopicBuilder.name(KafkaTopics.STATUS_CHANGED)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic resumeScoredTopic() {
        return TopicBuilder.name(KafkaTopics.RESUME_SCORED)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic reminderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.REMINDER_CREATED)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic digestGeneratedTopic() {
        return TopicBuilder.name(KafkaTopics.DIGEST_GENERATED)
                .partitions(1).replicas(1).build();
    }

    // ── DLT topics — one per main topic ──────────────────────────────────────
    // FIX: previously only APPLICATION_CREATED and STATUS_CHANGED had DLT beans.
    // With auto-create disabled, the missing DLTs for RESUME_SCORED,
    // REMINDER_CREATED, and DIGEST_GENERATED would cause producer failures
    // the first time a consumer on those topics hit a processing error.

    @Bean
    public NewTopic applicationCreatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_CREATED_DLT)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic statusChangedDltTopic() {
        return TopicBuilder.name(KafkaTopics.STATUS_CHANGED_DLT)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic resumeScoredDltTopic() {
        return TopicBuilder.name(KafkaTopics.RESUME_SCORED_DLT)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic reminderCreatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.REMINDER_CREATED_DLT)
                .partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic digestGeneratedDltTopic() {
        return TopicBuilder.name(KafkaTopics.DIGEST_GENERATED_DLT)
                .partitions(1).replicas(1).build();
    }

    /**
     * Global error handler for all @KafkaListener containers.
     * On failure: exponential backoff 1s → 2s → 4s (3 retries = 4 total attempts).
     * After retries exhausted: route to the corresponding DLT.
     * All DLT topic names follow the same convention: original + ".DLT"
     * This matches the NewTopic beans defined above — no routing ambiguity.
     * Partition is resolved dynamically from the original record rather than
     * hardcoded to 0, so this works correctly if DLT topics ever get multiple
     * partitions in the future.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                // FIX: removed hardcoded partition(0). Let the recoverer resolve
                // the partition dynamically from the original record's partition.
                // All topics currently have 1 partition so this is equivalent,
                // but it won't break if partitions are increased later.
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    int partition = record.partition(); // dynamic, not hardcoded 0
                    return new TopicPartition(dltTopic, partition);
                }
        );

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}