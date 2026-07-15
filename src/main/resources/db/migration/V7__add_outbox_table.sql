-- V7__add_outbox_table.sql
-- Transactional Outbox pattern: events are written to this table within
-- the same transaction as the domain change. A poller reads unpublished
-- rows and sends them to Kafka, then marks them published.
--
-- This eliminates the window where a crash between DB commit and Kafka
-- publish would lose the event (the publishAfterCommit problem).

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    published_at    TIMESTAMP,
    published       BOOLEAN NOT NULL DEFAULT FALSE
);

-- Partial index: only unpublished rows are queried by the poller.
-- Published rows are invisible to the hot path.
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at)
    WHERE published = FALSE;
