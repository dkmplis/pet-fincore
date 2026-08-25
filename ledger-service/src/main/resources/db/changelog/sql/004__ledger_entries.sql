-- liquibase formatted sql
-- changeset dkmplis:004
CREATE TABLE ledger_entries
(
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    side VARCHAR(6) NOT NULL,
    amount_minor BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ledger_entry_transaction
        FOREIGN KEY (transaction_id)
            REFERENCES ledger_transactions(id),

    CONSTRAINT fk_ledger_entry_account
        FOREIGN KEY (account_id)
            REFERENCES ledger_accounts(id),

    CONSTRAINT chk_ledger_entry_side
        CHECK (
            side IN ('DEBIT', 'CREDIT')
            ),

    CONSTRAINT chk_ledger_entry_amount
        CHECK (
            amount_minor > 0
            )
);

CREATE INDEX idx_ledger_entries_account
    ON ledger_entries(account_id);

CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries(transaction_id);