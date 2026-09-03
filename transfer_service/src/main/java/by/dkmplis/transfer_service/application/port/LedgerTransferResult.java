package by.dkmplis.transfer_service.application.port;

import java.util.UUID;

public record LedgerTransferResult(
        UUID ledgerTransactionId,
        boolean replayed
) {
}
