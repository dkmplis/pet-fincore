package by.dkmplis.ledgerservice.support;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.Posting;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.PostingSide;

import java.util.List;
import java.util.UUID;

public final class CommandFactory {

    public static PostLedgerTransactionCommand twoLegCommand(
            UUID operationId,
            UUID fromAccountId,
            UUID toAccountId,
            long amountMinor,
            LedgerTransactionType type
    ) {
        return new PostLedgerTransactionCommand(
                operationId,
                type,
                "BYN",
                null,
                List.of(
                        new Posting(
                                fromAccountId,
                                PostingSide.DEBIT,
                                amountMinor
                        ),
                        new Posting(
                                toAccountId,
                                PostingSide.CREDIT,
                                amountMinor
                        )
                )
        );
    }
}
