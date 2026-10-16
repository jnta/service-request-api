CREATE TABLE solicitations (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    service_type VARCHAR(40),
    title VARCHAR(80),
    description VARCHAR(1000),
    cep VARCHAR(8),
    street TEXT,
    neighborhood TEXT,
    city TEXT,
    state VARCHAR(2),
    address_number VARCHAR(20),
    complement VARCHAR(120),
    priority VARCHAR(10),
    preferred_date DATE,
    estimated_value_minor_units BIGINT,
    estimated_value_currency VARCHAR(3),
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    decision VARCHAR(10),
    decision_comment TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_solicitations_client
        FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT chk_solicitations_cep_length CHECK (cep IS NULL OR char_length(cep) = 8),
    CONSTRAINT chk_solicitations_state_length CHECK (state IS NULL OR char_length(state) = 2),
    CONSTRAINT chk_solicitations_version CHECK (version >= 0)
);

CREATE INDEX idx_solicitations_client_id ON solicitations (client_id);
