-- liquibase formatted sql
-- changeset dkmplis:002

CREATE TABLE outbox_events
(
    id              UUID         PRIMARY KEY,

    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    UUID         NOT NULL,

    event_type      VARCHAR(128) NOT NULL,
    event_version   INTEGER      NOT NULL,

    topic           VARCHAR(128) NOT NULL,
    payload         TEXT         NOT NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,

    attempts        INTEGER      NOT NULL DEFAULT 0,
    last_error      TEXT,

    CONSTRAINT chk_outbox_event_version_positive
        CHECK (event_version > 0),

    CONSTRAINT chk_outbox_attempts_non_negative
        CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;