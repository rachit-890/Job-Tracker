package com.rachit.jobtrackr.integration;

import com.rachit.jobtrackr.consumer.IdempotencyGuard;
import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.AnalyticsSnapshot;
import com.rachit.jobtrackr.event.ApplicationCreatedPayload;
import com.rachit.jobtrackr.event.EventEnvelope;
import com.rachit.jobtrackr.event.KafkaTopics;
import com.rachit.jobtrackr.repository.AnalyticsSnapshotRepository;
import com.rachit.jobtrackr.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration tests for Kafka consumer idempotency.
 *
 * Verifies:
 * 1. First delivery of an event is processed and recorded in processed_events
 * 2. Duplicate delivery (same eventId + consumerGroup) is skipped — no double-counting
 * 3. Same eventId with different consumer group is processed independently
 */
class ConsumerIdempotencyIT extends IntegrationTestBase {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @Autowired
    private IdempotencyGuard idempotencyGuard;

    private int baselineTotal;

    @BeforeEach
    void captureBaseline() {
        baselineTotal = analyticsSnapshotRepository
                .findById(AnalyticsSnapshot.SINGLETON_ID)
                .map(AnalyticsSnapshot::getTotalApplications)
                .orElse(0);
    }

    @Test
    @DisplayName("First event delivery is processed and recorded in processed_events table")
    void firstDelivery_shouldProcessAndRecord() {
        UUID applicationId = UUID.randomUUID();
        ApplicationCreatedPayload payload = new ApplicationCreatedPayload(
                applicationId, "TestCorp", "Engineer",
                "JD text", "v1", null, LocalDate.now()
        );
        EventEnvelope<ApplicationCreatedPayload> envelope =
                EventEnvelope.of("ApplicationCreatedEvent", payload);

        kafkaTemplate.send(KafkaTopics.APPLICATION_CREATED,
                applicationId.toString(), envelope);

        // Wait for the analytics consumer to process it
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            AnalyticsSnapshot snapshot = analyticsSnapshotRepository
                    .findById(AnalyticsSnapshot.SINGLETON_ID).orElseThrow();
            assertThat(snapshot.getTotalApplications())
                    .isGreaterThan(baselineTotal);
        });

        // Verify processed_events row exists for the analytics consumer group
        assertThat(processedEventRepository.existsByIdConsumerGroupAndIdEventId(
                "jobtrackr.analytics-consumer", envelope.eventId()
        )).isTrue();
    }

    @Test
    @DisplayName("Duplicate event with same eventId is skipped — no double counting")
    void duplicateDelivery_shouldBeSkipped() {
        UUID applicationId = UUID.randomUUID();
        UUID fixedEventId = UUID.randomUUID();

        ApplicationCreatedPayload payload = new ApplicationCreatedPayload(
                applicationId, "DuplicateCorp", "Tester",
                "Test JD", "v1", null, LocalDate.now()
        );

        // Create envelope with a fixed eventId
        EventEnvelope<ApplicationCreatedPayload> envelope = new EventEnvelope<>(
                fixedEventId, UUID.randomUUID(), 1,
                java.time.Instant.now(), "jobtrackr-service",
                "ApplicationCreatedEvent", payload
        );

        // Send the event first time
        kafkaTemplate.send(KafkaTopics.APPLICATION_CREATED,
                applicationId.toString(), envelope);

        // Wait for processing
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.existsByIdConsumerGroupAndIdEventId(
                    "jobtrackr.analytics-consumer", fixedEventId
            )).isTrue();
        });

        // Capture count after first processing
        int countAfterFirst = analyticsSnapshotRepository
                .findById(AnalyticsSnapshot.SINGLETON_ID)
                .map(AnalyticsSnapshot::getTotalApplications)
                .orElse(0);

        // Send the SAME event again (duplicate)
        kafkaTemplate.send(KafkaTopics.APPLICATION_CREATED,
                applicationId.toString(), envelope);

        // Wait a bit and verify the count did NOT increment again
        // (The duplicate should be skipped by IdempotencyGuard)
        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

        int countAfterDuplicate = analyticsSnapshotRepository
                .findById(AnalyticsSnapshot.SINGLETON_ID)
                .map(AnalyticsSnapshot::getTotalApplications)
                .orElse(0);

        assertThat(countAfterDuplicate).isEqualTo(countAfterFirst);
    }

    @Test
    @DisplayName("Same eventId processed independently by different consumer groups")
    void sameEventId_differentConsumerGroups_shouldProcessIndependently() {
        UUID eventId = UUID.randomUUID();
        String group1 = "test-group-alpha";
        String group2 = "test-group-beta";

        // First group processes — should return false (not already processed)
        boolean alreadyProcessedGroup1 = idempotencyGuard.alreadyProcessed(group1, eventId);
        assertThat(alreadyProcessedGroup1).isFalse();

        // Second group processes same eventId — should also return false
        boolean alreadyProcessedGroup2 = idempotencyGuard.alreadyProcessed(group2, eventId);
        assertThat(alreadyProcessedGroup2).isFalse();

        // Now both should be marked as processed
        assertThat(idempotencyGuard.alreadyProcessed(group1, eventId)).isTrue();
        assertThat(idempotencyGuard.alreadyProcessed(group2, eventId)).isTrue();
    }
}
