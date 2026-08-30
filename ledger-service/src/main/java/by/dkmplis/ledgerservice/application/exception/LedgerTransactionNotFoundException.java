package by.dkmplis.ledgerservice.application.exception;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class LedgerTransactionNotFoundException extends EntityNotFoundException {
    public LedgerTransactionNotFoundException(UUID transactionId) {
        super(
                "Ledger transaction not found: %s"
                        .formatted(transactionId)
        );
    }
}
