package com.proj.jobtracker.consumer;


import com.proj.jobtracker.entity.ProcessedEvent;
import com.proj.jobtracker.entity.ProcessedEventId;
import com.proj.jobtracker.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class IdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyGuard.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyGuard(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Returns true if this (consumerGroup, eventId) has already been processed
     * — the caller should skip the event and ack immediately.
     *
     * Returns false and records the pair atomically if it hasn't been seen yet
     * — the caller should process and then ack.
     *
     * REQUIRES_NEW: runs in its own transaction, independent of the consumer's
     * transaction. If the consumer's transaction rolls back due to a processing
     * error, the idempotency record is NOT rolled back — so the next retry still
     * sees "already processed" and skips cleanly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean alreadyProcessed(String consumerGroup, UUID eventId) {
        ProcessedEventId id = new ProcessedEventId(consumerGroup, eventId);

        if (processedEventRepository.existsById(id)) {
            log.warn("Duplicate event skipped: consumerGroup={} eventId={}", consumerGroup, eventId);
            return true;
        }

        try {
            processedEventRepository.save(new ProcessedEvent(id, Instant.now()));
            return false;
        } catch (DataIntegrityViolationException e) {
            // Two consumer instances raced — the other won. Treat as already processed.
            log.warn("Concurrent duplicate skipped: consumerGroup={} eventId={}", consumerGroup, eventId);
            return true;
        }
    }
}