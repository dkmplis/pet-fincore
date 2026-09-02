package by.dkmplis.ledgerservice.api.dto;

public enum LedgerErrorCode {
    LEDGER_TRANSACTION_NOT_FOUND,
    INSUFFICIENT_FUNDS,
    IDEMPOTENCY_CONFLICT,
    LEDGER_TRANSACTION_ALREADY_REVERSED,
    INVALID_REQUEST
}
