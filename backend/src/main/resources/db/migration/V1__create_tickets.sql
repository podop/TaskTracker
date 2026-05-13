CREATE TABLE tickets (
    id            UUID PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(4000),
    status        VARCHAR(32)  NOT NULL,
    assignee_id   VARCHAR(64),
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT ck_tickets_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE_CANDIDATE', 'DONE'))
);

CREATE INDEX ix_tickets_status     ON tickets (status);
CREATE INDEX ix_tickets_updated_at ON tickets (updated_at DESC);
