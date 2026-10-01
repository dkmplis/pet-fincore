-- liquibase formatted sql
-- changeset dkmplis:005

CREATE TABLE inbox_events
(
    event_id       UUID PRIMARY KEY,
    event_type     VARCHAR(128) NOT NULL,
    event_version  INTEGER NOT NULL,
    aggregate_id   UUID NOT NULL,
    payload        TEXT NOT NULL,

    received_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ,

    CONSTRAINT chk_transfer_inbox_event_version_positive
        CHECK (event_version > 0)
);

CREATE INDEX idx_transfer_inbox_events_aggregate_id
    ON inbox_events (aggregate_id);