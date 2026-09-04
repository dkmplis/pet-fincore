package by.dkmplis.transfer_service.api.dto;

import by.dkmplis.transfer_service.domain.enums.TransferState;

import java.util.UUID;

public record TransferDetailsResult(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor,
        TransferState state
) {
}
