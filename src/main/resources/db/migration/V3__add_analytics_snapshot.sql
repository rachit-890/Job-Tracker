-- V3__add_analytics_snapshot.sql
-- Analytics snapshot table. Maintained by the analytics Kafka consumer.
-- Dashboard reads come from here (pre-aggregated) rather than from raw rows.
-- Using a singleton row — one row represents the current state of the
-- entire dataset. Phase 5 adds time-series snapshots for trend data.

CREATE TABLE analytics_snapshot (
                                    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    total_applications   INT  NOT NULL DEFAULT 0,
                                    applied_count        INT  NOT NULL DEFAULT 0,
                                    screening_count      INT  NOT NULL DEFAULT 0,
                                    interview_count      INT  NOT NULL DEFAULT 0,
                                    offer_count          INT  NOT NULL DEFAULT 0,
                                    rejected_count       INT  NOT NULL DEFAULT 0,
                                    stale_count          INT  NOT NULL DEFAULT 0,
                                    computed_at          TIMESTAMP NOT NULL
);

-- Seed the singleton row with zeros. The analytics consumer updates this row
-- on every relevant event; it never inserts a second row.
INSERT INTO analytics_snapshot (
    id, total_applications, applied_count, screening_count,
    interview_count, offer_count, rejected_count, stale_count, computed_at
) VALUES (
             'a0000000-0000-0000-0000-000000000001',
             0, 0, 0, 0, 0, 0, 0,
             now()
         );