-- liquibase formatted sql

--changeset dkmplis:005-01 splitStatements:false

CREATE OR REPLACE FUNCTION reject_ledger_entry_mutation()
    RETURNS TRIGGER
AS $$
BEGIN
    RAISE EXCEPTION
        'Ledger entry mutation is forbidden: %',
        TG_OP;
END;
$$ LANGUAGE plpgsql;

--changeset dkmplis:005-02

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE
    ON ledger_entries
    FOR EACH ROW
EXECUTE FUNCTION reject_ledger_entry_mutation();

--changeset dkmplis:005-03 splitStatements:false

CREATE OR REPLACE FUNCTION guard_ledger_transaction_mutation()
    RETURNS TRIGGER
AS $$
BEGIN

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Ledger transaction deletion is forbidden';
    END IF;

    IF OLD.state = 'DRAFT'
        AND NEW.state = 'POSTED'

        AND NEW.id IS NOT DISTINCT FROM OLD.id
        AND NEW.external_operation_id
           IS NOT DISTINCT FROM OLD.external_operation_id
        AND NEW.transaction_type
           IS NOT DISTINCT FROM OLD.transaction_type
        AND NEW.currency
           IS NOT DISTINCT FROM OLD.currency
        AND NEW.request_fingerprint
           IS NOT DISTINCT FROM OLD.request_fingerprint
        AND NEW.reverses_transaction_id
           IS NOT DISTINCT FROM OLD.reverses_transaction_id
        AND NEW.created_at
           IS NOT DISTINCT FROM OLD.created_at
    THEN
        RETURN NEW;
    END IF;

    RAISE EXCEPTION
        'Illegal ledger transaction mutation: % -> %',
        OLD.state,
        NEW.state;
END;
$$ LANGUAGE plpgsql;

--changeset dkmplis:005-04

DROP TRIGGER IF EXISTS trg_ledger_transactions_immutable
    ON ledger_transactions;


CREATE TRIGGER trg_ledger_transactions_immutable
    BEFORE UPDATE OR DELETE
    ON ledger_transactions
    FOR EACH ROW
EXECUTE FUNCTION guard_ledger_transaction_mutation();