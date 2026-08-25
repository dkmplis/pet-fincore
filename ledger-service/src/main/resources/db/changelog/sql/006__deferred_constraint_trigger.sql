-- liquibase formatted sql

--changeset dkmplis:006-01 splitStatements:false

CREATE OR REPLACE FUNCTION validate_ledger_transaction()
RETURNS TRIGGER
AS $$
DECLARE
    debit_total NUMERIC;
    credit_total NUMERIC;
    posting_count BIGINT;
    transaction_state VARCHAR(16);
BEGIN

    SELECT state
    INTO transaction_state
    FROM ledger_transactions
    WHERE id = NEW.id;

    IF transaction_state <> 'POSTED' THEN
        RAISE EXCEPTION
            'Ledger transaction % must be POSTED before commit',
            NEW.id;
    END IF;

    SELECT
        COALESCE(
            SUM(amount_minor)
                FILTER (WHERE side = 'DEBIT'),
            0
        ),
        COALESCE(
            SUM(amount_minor)
                FILTER (WHERE side = 'CREDIT'),
            0
        ),
        COUNT(*)
    INTO
        debit_total,
        credit_total,
        posting_count
    FROM ledger_entries
    WHERE transaction_id = NEW.id;

    IF posting_count < 2 THEN
        RAISE EXCEPTION
            'Ledger transaction % must contain at least two postings',
            NEW.id;
    END IF;

    IF debit_total <> credit_total THEN
        RAISE EXCEPTION
            'Unbalanced transaction %. debit=%, credit=%',
            NEW.id,
            debit_total,
            credit_total;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--changeset dkmplis:006-02

CREATE CONSTRAINT TRIGGER trg_validate_ledger_transaction
AFTER INSERT
ON ledger_transactions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_ledger_transaction();