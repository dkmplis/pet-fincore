-- liquibase formatted sql
-- changeset dkmplis:003
CREATE TABLE ledger_transactions
(
    id UUID PRIMARY KEY,
    external_operation_id UUID NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(3) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    reverses_transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_ledger_external_operation
        UNIQUE (external_operation_id),

    CONSTRAINT fk_ledger_transaction_reversal
        FOREIGN KEY (reverses_transaction_id)
            REFERENCES ledger_transactions(id),

    CONSTRAINT chk_ledger_transaction_type
        CHECK (
            transaction_type IN (
                                 'FUNDING',
                                 'TRANSFER',
                                 'WITHDRAWAL',
                                 'FEE',
                                 'REFUND',
                                 'REVERSAL'
            )
        ),

    CONSTRAINT chk_ledger_transaction_currency
        CHECK (
            currency ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_ledger_transaction_state
        CHECK (
            state IN ('DRAFT', 'POSTED')
        ),

    CHECK ( request_fingerprint ~ '^[0-9a-f]{64}$'),
    CHECK (
        (transaction_type = 'REVERSAL'
            AND reverses_transaction_id IS NOT NULL)
            OR
        (transaction_type <> 'REVERSAL'
            AND reverses_transaction_id IS NULL)
    ),
    CHECK (
        reverses_transaction_id IS NULL
            OR reverses_transaction_id <> id
    )
);

CREATE INDEX idx_ledger_transactions_created_at
    ON ledger_transactions(created_at);
CREATE UNIQUE INDEX uk_ledger_single_reversal
    ON ledger_transactions(reverses_transaction_id)
    WHERE reverses_transaction_id IS NOT NULL;