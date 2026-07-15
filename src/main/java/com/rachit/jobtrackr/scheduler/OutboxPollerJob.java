package com.rachit.jobtrackr.scheduler;

import com.rachit.jobtrackr.entity.OutboxEvent;
import com.rachit.jobtrackr.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Outbox Poller — reads unpublished events from the outbox table and
 * sends them to Kafka, then marks them as published.
 *
 * Why a poller instead of CDC (Debezium)?
 * For a personal job tracker, a simple poll loop is far easier to operate
 * than a full CDC pipeline. The tradeoff is slightly higher latency
 * (up to 500ms) vs. zero latency with CDC. Acceptable here.
 *
 * Delivery guarantee: at-least-once.
 * If the poller publishes to Kafka but crashes before marking the row
 * published, the next poll will re-send it. Consumers already handle
 * duplicates via IdempotencyGuard.
 *
 * Cleanup: published events older than 7 days are deleted.
 * This keeps the table small. For production audit needs, consider
 * archiving to a cold table or extending to 30 days.
 */
@Component
public class OutboxPollerJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerJob.class);
    private static final int BATCH_SIZE = 50;
    private static final int CLEANUP_RETENTION_DAYS = 7;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxPollerJob(OutboxRepository outboxRepository,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Poll every 500ms for unpublished outbox events.
     * Sends each to Kafka using the stored topic and aggregateId as key.
     *
     * The payload is sent as a raw JSON string — KafkaTemplate's
     * JsonSerializer will serialize it. Since the payload is already
     * a JSON string from EventPublisher.writeToOutbox(), we send it directly.
     */
    @Scheduled(fixedDelay = 500)
    @SchedulerLock(name = "outbox-poller", lockAtLeastFor = "PT0S", lockAtMostFor = "PT1M")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> batch = outboxRepository.findUnpublishedBatch(BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("[OutboxPoller] Processing {} unpublished events", batch.size());

        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("[OutboxPoller] Kafka send failed for outboxId={} topic={}",
                                        event.getId(), event.getTopic(), ex);
                            }
                        });

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);

                log.debug("[OutboxPoller] Published outboxId={} topic={} key={}",
                        event.getId(), event.getTopic(), event.getAggregateId());
            } catch (Exception e) {
                log.error("[OutboxPoller] Failed to process outboxId={}: {}",
                        event.getId(), e.getMessage());
                // Don't mark published — will be retried on next poll
            }
        }
    }

    /**
     * Cleanup published events older than 7 days.
     * Runs once per hour to keep the outbox table small.
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @SchedulerLock(name = "outbox-cleanup", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(CLEANUP_RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("[OutboxPoller] Cleaned up {} published events older than {} days",
                    deleted, CLEANUP_RETENTION_DAYS);
        }
    }
}
