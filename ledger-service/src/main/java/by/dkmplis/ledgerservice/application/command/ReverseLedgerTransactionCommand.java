package by.dkmplis.ledgerservice.application.command;

import java.util.UUID;

public record ReverseLedgerTransactionCommand(
        UUID externalOperationId,
        UUID originalTransactionId
) {
}
