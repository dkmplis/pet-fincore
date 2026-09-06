package by.dkmplis.transfer_service.application.command;

import java.util.UUID;
import java.util.regex.Pattern;

public record CreateTransferCommand(
        UUID externalOperationId,
        UUID fromAccountId,
        UUID toAccountId,
        String currency,
        long amountMinor
) {
    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("^[A-Z]{3}$");

    public CreateTransferCommand {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Transfer accounts must be different"
            );
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive"
            );
        }

        if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            throw new IllegalArgumentException(
                    "Currency must contain 3 uppercase characters"
            );
        }
    }
}
