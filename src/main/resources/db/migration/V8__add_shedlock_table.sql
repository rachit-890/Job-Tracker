-- V8__add_shedlock_table.sql
-- ShedLock distributed lock table.
-- Prevents duplicate execution of scheduled jobs when running
-- multiple application instances behind a load balancer.
--
-- Each scheduled job acquires a row-level lock before executing.
-- If another instance already holds the lock, the job is skipped.

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
