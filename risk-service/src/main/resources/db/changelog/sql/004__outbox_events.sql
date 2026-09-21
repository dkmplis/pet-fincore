-- liquibase formatted sql
-- changeset dkmplis:004

CREATE TABLE outbox_events
(
    id              UUID         PRIMARY KEY,
    event_id        UUID         NOT NULL,

    topic           VARCHAR(128) NOT NULL,
    event_key       VARCHAR(128) NOT NULL,

    event_type      VARCHAR(128) NOT NULL,
    event_version   INTEGER      NOT NULL,
    aggregate_id    UUID         NOT NULL,

    payload         TEXT         NOT NULL,

    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ,

    attempts        INTEGER      NOT NULL DEFAULT 0,
    claimed_at      TIMESTAMPTZ,
    claim_token     UUID,
    last_error      TEXT,

    CONSTRAINT uq_risk_outbox_event_id
        UNIQUE (event_id),

    CONSTRAINT chk_risk_outbox_event_version_positive
        CHECK (event_version > 0),

    CONSTRAINT chk_risk_outbox_attempts_non_negative
        CHECK (attempts >= 0),

    CONSTRAINT chk_risk_outbox_claim_consistency
        CHECK (
            (claimed_at IS NULL AND claim_token IS NULL)
                OR
            (claimed_at IS NOT NULL AND claim_token IS NOT NULL)
            ),

    CONSTRAINT chk_risk_outbox_published_not_claimed
        CHECK (
            published_at IS NULL
                OR
            (claimed_at IS NULL AND claim_token IS NULL)
            )
);

CREATE INDEX idx_risk_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;

CREATE INDEX idx_risk_outbox_events_claimed
    ON outbox_events (claimed_at)
    WHERE published_at IS NULL
      AND claimed_at IS NOT NULL;

CREATE INDEX idx_risk_outbox_events_aggregate
    ON outbox_events (aggregate_id, created_at);