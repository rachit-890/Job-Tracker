-- V11__add_goals.sql
-- Application goals feature.
-- Allows users to set a target number of applications per week/month.

CREATE TABLE application_goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_count    INT NOT NULL,
    period          VARCHAR(20) NOT NULL DEFAULT 'WEEKLY' CHECK (period IN ('WEEKLY', 'MONTHLY')),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_goals_active ON application_goals(active) WHERE active = TRUE;
