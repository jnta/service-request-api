CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (user_role IN ('CLIENT', 'ANALYST', 'ADMIN'))
);

CREATE TABLE analyst_states (
    analyst_id UUID NOT NULL REFERENCES users (id),
    state VARCHAR(2) NOT NULL,
    PRIMARY KEY (analyst_id, state),
    CONSTRAINT analyst_states_state_check CHECK (state IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
        'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
    ))
);

CREATE TABLE solicitations (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users (id),
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
    estimated_value NUMERIC,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    decision VARCHAR(10),
    decision_comment TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT solicitations_basic_data_complete CHECK (
        (service_type IS NULL AND title IS NULL AND description IS NULL)
        OR (service_type IS NOT NULL AND title IS NOT NULL AND description IS NOT NULL)
    ),
    CONSTRAINT solicitations_address_complete CHECK (
        (cep IS NULL AND street IS NULL AND neighborhood IS NULL AND city IS NULL
            AND state IS NULL AND address_number IS NULL)
        OR (cep IS NOT NULL AND street IS NOT NULL AND neighborhood IS NOT NULL AND city IS NOT NULL
            AND state IS NOT NULL AND address_number IS NOT NULL)
    ),
    CONSTRAINT solicitations_confirmation_complete CHECK (
        (priority IS NULL AND preferred_date IS NULL AND estimated_value IS NULL AND terms_accepted = FALSE)
        OR (priority IS NOT NULL AND preferred_date IS NOT NULL AND estimated_value IS NOT NULL)
    ),
    CONSTRAINT solicitations_state_check CHECK (state IS NULL OR state IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
        'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
    )),
    CONSTRAINT solicitations_priority_check CHECK (
        priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH')
    ),
    CONSTRAINT solicitations_status_check CHECK (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    CONSTRAINT solicitations_decision_check CHECK (
        (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW') AND decision IS NULL)
        OR (status = 'APPROVED' AND decision = 'APPROVE')
        OR (status = 'REJECTED' AND decision = 'REJECT')
    ),
    CONSTRAINT solicitations_submitted_complete CHECK (
        status = 'DRAFT'
        OR (service_type IS NOT NULL AND title IS NOT NULL AND description IS NOT NULL
            AND cep IS NOT NULL AND street IS NOT NULL AND neighborhood IS NOT NULL
            AND city IS NOT NULL AND state IS NOT NULL AND address_number IS NOT NULL
            AND priority IS NOT NULL AND preferred_date IS NOT NULL AND estimated_value IS NOT NULL
            AND terms_accepted = TRUE)
    ),
    CONSTRAINT solicitations_estimated_value_check CHECK (
        estimated_value IS NULL OR estimated_value >= 0
    ),
    CONSTRAINT solicitations_high_priority_value_check CHECK (
        priority IS DISTINCT FROM 'HIGH' OR estimated_value >= 100
    )
);

CREATE INDEX solicitations_client_id_idx ON solicitations (client_id);
CREATE INDEX solicitations_status_idx ON solicitations (status);
CREATE INDEX solicitations_state_idx ON solicitations (state);
