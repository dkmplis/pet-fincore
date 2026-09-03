package by.dkmplis.transfer_service.infrastructure.client.ledger.dto;

import java.util.UUID;

public record LedgerTransactionResponse(
        UUID transactionId,
        boolean replayed
) {
}
