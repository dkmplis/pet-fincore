-- liquibase formatted sql
-- changeset dkmplis:001

CREATE TABLE transfers
(
    id                      UUID        PRIMARY KEY,

    external_operation_id   UUID        NOT NULL,
    request_fingerprint     VARCHAR(64) NOT NULL,

    ledger_operation_id     UUID        NOT NULL,
    ledger_transaction_id   UUID,

    from_account_id         UUID        NOT NULL,
    to_account_id           UUID        NOT NULL,

    currency                VARCHAR(3)  NOT NULL,
    amount_minor            BIGINT      NOT NULL,

    state                   VARCHAR(16) NOT NULL,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_transfers_external_operation
        UNIQUE (external_operation_id),

    CONSTRAINT uq_transfers_ledger_operation
        UNIQUE (ledger_operation_id),

    CONSTRAINT chk_transfers_different_accounts
        CHECK (from_account_id <> to_account_id),

    CONSTRAINT chk_transfers_amount_positive
        CHECK (amount_minor > 0),

    CONSTRAINT chk_transfers_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_transfers_state
        CHECK (state IN ('PENDING', 'COMPLETED', 'REJECTED'))
);