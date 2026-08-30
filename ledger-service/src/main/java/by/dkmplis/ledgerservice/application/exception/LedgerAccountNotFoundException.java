package by.dkmplis.ledgerservice.application.exception;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class LedgerAccountNotFoundException
        extends EntityNotFoundException {

    public LedgerAccountNotFoundException(UUID accountId) {
        super("Ledger account not found: %s".formatted(accountId));
    }
}
