CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_pending_created_at
    ON outbox_events (created_at)
    WHERE processed_at IS NULL;
