ALTER TABLE orders ADD COLUMN order_type    VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE';
ALTER TABLE orders ADD COLUMN scheduled_pickup_at TIMESTAMP;

-- Partial index — only rows still in SCHEDULED state are indexed.
-- Drops out automatically when status changes to PENDING at dispatch time.
CREATE INDEX idx_orders_scheduled_dispatch
    ON orders (scheduled_pickup_at)
    WHERE status = 'SCHEDULED';

-- ShedLock table — prevents double-dispatch when multiple backend instances run
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
