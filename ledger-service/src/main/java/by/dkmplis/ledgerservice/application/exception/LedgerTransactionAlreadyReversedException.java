package by.dkmplis.ledgerservice.application.exception;

import java.util.UUID;

public class LedgerTransactionAlreadyReversedException extends IllegalStateException {
    public LedgerTransactionAlreadyReversedException(UUID transactionId) {
        super(
                "Ledger transaction is already reversed: %s"
                        .formatted(transactionId)
        );
    }
}
