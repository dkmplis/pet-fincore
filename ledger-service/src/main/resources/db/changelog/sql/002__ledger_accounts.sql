-- liquibase formatted sql
-- changeset dkmplis:002
CREATE TABLE ledger_accounts
(
    id UUID PRIMARY KEY,
    account_class VARCHAR(16) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    allow_negative BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_ledger_account_class
        CHECK (
            account_class IN (
                'ASSET',
                'LIABILITY',
                'EQUITY',
                'REVENUE',
                'EXPENSE'
            )
        ),
    CONSTRAINT chk_ledger_account_status
        CHECK (
            status IN (
                'ACTIVE',
                'BLOCKED',
                'CLOSED'
            )
        ),
    CONSTRAINT chk_ledger_account_purpose
        CHECK (
            purpose IN (
                'CUSTOMER_FUNDS',
                'SETTLEMENT',
                'PLATFORM_EQUITY',
                'FEE_REVENUE'
            )
        ),
    CONSTRAINT chk_ledger_account_currency
        CHECK (
            currency ~ '^[A-Z]{3}$'
        ),
    CONSTRAINT chk_account_purpose_class
        CHECK (
            (purpose = 'CUSTOMER_FUNDS'
                AND account_class = 'LIABILITY')

                OR (purpose = 'SETTLEMENT'
                AND account_class = 'ASSET')

                OR (purpose = 'PLATFORM_EQUITY'
                AND account_class = 'EQUITY')

                OR (purpose = 'FEE_REVENUE'
                AND account_class = 'REVENUE')
        )
);