package by.dkmplis.ledgerservice.api.dto;

import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.PostingSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record PostLedgerTransactionRequest(
        @NotNull
        LedgerTransactionType transactionType,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,
        @NotNull
        @Size(min = 2)
        List<@Valid PostingRequest> postings
) {
    public record PostingRequest(
            @NotNull
            UUID accountId,
            @NotNull
            PostingSide side,
            @Positive
            long amountMinor
    ) {
    }
}
