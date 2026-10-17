CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    success BOOLEAN NOT NULL
);

CREATE INDEX idx_audit_logs_aggregate_occurred_at
    ON audit_logs (aggregate_type, aggregate_id, occurred_at);

CREATE INDEX idx_audit_logs_actor_occurred_at
    ON audit_logs (actor_id, occurred_at);
