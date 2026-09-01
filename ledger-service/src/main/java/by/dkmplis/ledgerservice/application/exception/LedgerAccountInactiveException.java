package by.dkmplis.ledgerservice.application.exception;

import java.util.UUID;

public class LedgerAccountInactiveException
        extends RuntimeException{

    public LedgerAccountInactiveException(UUID accountId) {
        super("Ledger account is not active: %s".formatted(accountId));
    }
}
