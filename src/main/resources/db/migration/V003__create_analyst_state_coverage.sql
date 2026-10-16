CREATE TABLE analyst_states (
    analyst_id UUID NOT NULL,
    state VARCHAR(2) NOT NULL,
    CONSTRAINT pk_analyst_states PRIMARY KEY (analyst_id, state),
    CONSTRAINT fk_analyst_states_user
        FOREIGN KEY (analyst_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_analyst_states_code_length CHECK (char_length(state) = 2)
);
