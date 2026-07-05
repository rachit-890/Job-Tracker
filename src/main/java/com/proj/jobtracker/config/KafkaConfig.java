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
 * AUTO_CREATE is disabled in docker-compose (KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=false)
 * so we own the topic lifecycle here. Each topic has 1 partition (correct for
 * single-user workload) and a replication factor of 1 (single-broker dev setup).
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic applicationCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic statusChangedTopic() {
        return TopicBuilder.name(KafkaTopics.STATUS_CHANGED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic resumeScoredTopic() {
        return TopicBuilder.name(KafkaTopics.RESUME_SCORED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic reminderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.REMINDER_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic digestGeneratedTopic() {
        return TopicBuilder.name(KafkaTopics.DIGEST_GENERATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic applicationCreatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_CREATED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic statusChangedDltTopic() {
        return TopicBuilder.name(KafkaTopics.STATUS_CHANGED_DLT).partitions(1).replicas(1).build();
    }

    /**
     * Error handler used by all @KafkaListener containers.
     *
     * On failure: exponential backoff — 1s, 2s, 4s — then route to the
     * dead-letter topic. The DLT topic name is derived automatically:
     * original topic name + ".DLT" (Spring's default).
     *
     * Why exponential backoff instead of fixed retry?
     * A transient downstream failure (DB momentarily unavailable, Gemini timeout)
     * is more likely to resolve if we wait progressively longer between retries
     * rather than hammering immediately — the same reason TCP uses it.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                // Route to explicit DLT topic for the two high-value topics;
                // all others go to the Spring-derived <topic>.DLT name.
                (record, ex) -> {
                    String topic = record.topic();
                    if (KafkaTopics.APPLICATION_CREATED.equals(topic)) {
                        return new TopicPartition(KafkaTopics.APPLICATION_CREATED_DLT, 0);
                    }
                    if (KafkaTopics.STATUS_CHANGED.equals(topic)) {
                        return new TopicPartition(KafkaTopics.STATUS_CHANGED_DLT, 0);
                    }
                    return new TopicPartition(topic + ".DLT", 0);
                }
        );

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3); // 3 retries → 4 total attempts before DLT

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
