-- liquibase formatted sql
-- changeset dkmplis:003

ALTER TABLE outbox_events
    ADD COLUMN claimed_at TIMESTAMPTZ,
    ADD COLUMN claim_token UUID;