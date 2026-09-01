package by.dkmplis.ledgerservice.api.dto;

import java.util.UUID;

public record LedgerTransactionResponse(
        UUID transactionId,
        boolean replayed
) {
}
