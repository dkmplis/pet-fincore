package by.dkmplis.transfer_service.application.event;

import java.util.UUID;

public record TransferCreatedPayload(
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor
) {
}
