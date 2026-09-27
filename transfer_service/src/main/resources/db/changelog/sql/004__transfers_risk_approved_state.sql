-- liquibase formatted sql
-- changeset dkmplis:004

ALTER TABLE transfers
    DROP CONSTRAINT chk_transfers_state;

ALTER TABLE transfers
    ADD CONSTRAINT  chk_transfers_state
        CHECK (
            state IN (
                'PENDING',
                'RISK_APPROVED',
                'COMPLETED',
                'REJECTED'
                )
            );