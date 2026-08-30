package by.dkmplis.ledgerservice.application.command;

import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.PostingSide;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public record PostLedgerTransactionCommand(
        UUID externalOperationId,
        LedgerTransactionType transactionType,
        String currency,
        UUID reversesTransactionId,
        List<Posting> postings
) {
    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("^[A-Z]{3}$");

    public PostLedgerTransactionCommand {
        postings = List.copyOf(postings);
        if (!CURRENCY_PATTERN.matcher(currency).matches()) {
            throw new IllegalArgumentException(
                    "Currency must have 3 ISO code characters"
            );
        }
        if (postings.size() < 2) {
            throw new IllegalArgumentException(
                    "At least two Postings are required to carry out accounting transactions."
            );
        }
        validateReversal(transactionType, reversesTransactionId);
        validateBalanced(postings);
    }

    private static void validateReversal(
            LedgerTransactionType transactionType,
            UUID reversesTransactionId
    ) {
        if (transactionType == LedgerTransactionType.REVERSAL
                && reversesTransactionId == null) {
            throw new IllegalArgumentException(
                    "REVERSAL requires reversesTransactionId"
            );
        }
        if (transactionType != LedgerTransactionType.REVERSAL
                && reversesTransactionId != null) {
            throw new IllegalArgumentException(
                    "Only REVERSAL may contain reversesTransactionId"
            );
        }
    }

    private static void validateBalanced(List<Posting> postings) {
        long debit = postings.stream()
                .filter(p -> p.side() == PostingSide.DEBIT)
                .mapToLong(Posting::amountMinor)
                .reduce(0L, Math::addExact);
        long credit = postings.stream()
                .filter(p -> p.side() == PostingSide.CREDIT)
                .mapToLong(Posting::amountMinor)
                .reduce(0L, Math::addExact);
        if (debit != credit) {
            throw new IllegalArgumentException(
                    "Unbalanced ledger transaction: debit=%d, credit=%d"
                            .formatted(debit, credit)
            );
        }
    }
}
