-- liquibase formatted sql
-- changeset dkmplis:002

CREATE TABLE risk_assessments
(
    id              UUID PRIMARY KEY,
    transfer_id     UUID NOT NULL,
    from_account_id UUID NOT NULL,
    to_account_id   UUID NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    amount_minor    BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_risk_assessments_transfer
        UNIQUE (transfer_id),

    CONSTRAINT chk_risk_accounts_different
        CHECK (from_account_id <> to_account_id),

    CONSTRAINT chk_risk_amount_positive
        CHECK (amount_minor > 0),

    CONSTRAINT chk_risk_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_risk_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);