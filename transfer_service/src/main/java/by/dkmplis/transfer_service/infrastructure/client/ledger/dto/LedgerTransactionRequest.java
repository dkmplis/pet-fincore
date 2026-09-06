package by.dkmplis.transfer_service.infrastructure.client.ledger.dto;

import java.util.List;
import java.util.UUID;

public record LedgerTransactionRequest(
        LedgerTransactionType transactionType,
        String currency,
        List<PostingRequest> postings

) {
    public record PostingRequest(
            UUID accountId,
            PostingSide side,
            long amountMinor
    ) {
    }
}
