package by.dkmplis.riskservice.kafka.dto;

import java.util.UUID;

public record TransferCreatedPayload(
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor
) {
}
