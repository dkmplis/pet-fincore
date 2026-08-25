--liquibase formatted sql

--changeset dkmplis:007-01 splitStatements:false

CREATE OR REPLACE FUNCTION validate_ledger_entry_insert()
RETURNS TRIGGER
AS $$
DECLARE
    transaction_state    VARCHAR(16);
    transaction_currency VARCHAR(3);
    account_currency     VARCHAR(3);
BEGIN

    SELECT
        t.state,
        t.currency,
        a.currency
    INTO
        transaction_state,
        transaction_currency,
        account_currency
    FROM ledger_transactions t
    JOIN ledger_accounts a
        ON a.id = NEW.account_id
    WHERE t.id = NEW.transaction_id
    FOR UPDATE OF t;

    IF transaction_state <> 'DRAFT' THEN
        RAISE EXCEPTION
            'Cannot append posting to sealed ledger transaction %',
            NEW.transaction_id;
    END IF;

    IF transaction_currency <> account_currency THEN
        RAISE EXCEPTION
            'Currency mismatch: transaction %, account %',
            transaction_currency,
            account_currency;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--changeset dkmplis:007-02

CREATE TRIGGER trg_validate_ledger_entry_insert
BEFORE INSERT
ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION validate_ledger_entry_insert();