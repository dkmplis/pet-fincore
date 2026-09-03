package by.dkmplis.transfer_service.application.port;

import java.util.UUID;

public record LedgerTransferCommand(
        UUID ledgerOperationId,
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor
) {
}
