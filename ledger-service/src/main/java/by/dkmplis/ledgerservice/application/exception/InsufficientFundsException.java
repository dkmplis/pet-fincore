package by.dkmplis.ledgerservice.application.exception;

import java.util.UUID;

public class InsufficientFundsException
        extends IllegalArgumentException{

    public InsufficientFundsException(
            UUID accountID,
            long currentBalance,
            long resultingBalance
    ) {
        super (
                "Insufficient funds on account %s: current=%d, resulting=%d"
                        .formatted(accountID, currentBalance, resultingBalance)
        );
    }
}
