package com.proj.jobtracker.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard wrapper around every Kafka event payload.
 *
 * Fields:
 *   eventId       — unique ID per event instance; consumers use this for idempotency.
 *   correlationId — ties together all events originating from a single HTTP request,
 *                   making async flows traceable in logs.
 *   eventVersion  — schema version of the payload; allows consumers to stay
 *                   compatible as events evolve without a flag day.
 *   timestamp     — when the event was produced.
 *   source        — which component produced this event (e.g. "application-service").
 *   eventType     — discriminator string matching the payload type name.
 *   payload       — the event-specific data.
 *
 * @JsonTypeInfo tells Jackson to include the concrete payload class in the
 * serialized JSON so the consumer can deserialize to the right type.
 */
public record EventEnvelope<T>(
        UUID eventId,
        UUID correlationId,
        int eventVersion,
        Instant timestamp,
        String source,
        String eventType,

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@payloadType")
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                UUID.randomUUID(),  // In Phase 3 each event gets its own correlationId.
                // Phase 4+ will thread the request correlationId through.
                1,
                Instant.now(),
                "jobtrackr-service",
                eventType,
                payload
        );
    }
}
