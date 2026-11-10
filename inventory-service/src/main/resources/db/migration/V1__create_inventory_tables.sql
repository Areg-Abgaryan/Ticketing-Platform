CREATE TABLE ticket_stock (
    event_id  UUID    PRIMARY KEY,
    total     INT     NOT NULL,
    available INT     NOT NULL,
    version   BIGINT  NOT NULL,
    -- last line of defence against overselling, whatever the application does
    CONSTRAINT ticket_stock_available_in_range CHECK (total > 0 AND available >= 0 AND available <= total)
);

CREATE TABLE reservation (
    id         UUID        PRIMARY KEY,
    event_id   UUID        NOT NULL REFERENCES ticket_stock (event_id),
    user_id    UUID        NOT NULL,
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL
);

-- "does this user already hold tickets for this event?"
CREATE INDEX reservation_held_by_user_and_event ON reservation (user_id, event_id) WHERE status = 'HELD';
-- "which holds have run out of time?"
CREATE INDEX reservation_held_by_expiry ON reservation (expires_at) WHERE status = 'HELD';
