package by.dkmplis.ledgerservice.application.exception;

import java.util.UUID;

public class LedgerCurrencyMismatchException
        extends IllegalArgumentException {

    public LedgerCurrencyMismatchException(
            UUID accountId,
            String expected,
            String actual
            ) {
        super(
                "Currency mismatch for account %s: expected=%s, actual=%s"
                        .formatted(accountId, expected, actual)
        );
    }
}
