-- liquibase formatted sql
-- changeset dkmplis:003

ALTER TABLE risk_assessments
    ADD COLUMN decision_reason VARCHAR(256),
    ADD COLUMN decided_at TIMESTAMPTZ;

ALTER TABLE risk_assessments
    ADD CONSTRAINT chk_risk_decision_consistency
        CHECK (
            (status = 'PENDING'
                AND decision_reason IS NULL
                AND decided_at IS NULL)
                OR
            (status IN ('APPROVED', 'REJECTED')
                AND decision_reason IS NOT NULL
                AND decided_at IS NOT NULL)
            );