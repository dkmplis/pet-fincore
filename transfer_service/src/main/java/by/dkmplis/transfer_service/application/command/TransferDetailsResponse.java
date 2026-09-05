package by.dkmplis.transfer_service.application.command;

import by.dkmplis.transfer_service.domain.enums.TransferState;

import java.util.UUID;

public record TransferDetailsResponse(
        UUID transferId,
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor,
        TransferState state
) {
}
