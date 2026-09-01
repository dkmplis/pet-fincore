package by.dkmplis.ledgerservice.application.command;

import java.util.UUID;

public record PostLedgerTransactionResult(
        UUID transactionId,
        boolean replayed
) {
}
