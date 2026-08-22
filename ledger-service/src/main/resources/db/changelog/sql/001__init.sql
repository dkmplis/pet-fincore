-- liquibase formatted sql
-- changeset dkmplis:001
CREATE TABLE ledger_schema_version_marker
(
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);