package by.dkmplis.transfer_service.infrastructure.client.ledger.dto;

import java.time.Instant;

public record LedgerApiError(
        int status,
        String code,
        String message,
        Instant timestamp
) {
}
