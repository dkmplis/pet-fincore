package by.dkmplis.transfer_service.application.event;

import java.util.UUID;

public record TransferCompletedPayload(
        UUID ledgerTransactionId
) {
}
