package by.dkmplis.ledgerservice.application.command;

import by.dkmplis.ledgerservice.domain.enums.PostingSide;

import java.util.UUID;

public record Posting(
        UUID accountId,
        PostingSide side,
        long amountMinor
) {
    public Posting {
        if (amountMinor <= 0) {
            throw new  IllegalArgumentException(
                    "Сумма должна быть положительной"
            );
        }
    }
}
