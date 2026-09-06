package by.dkmplis.transfer_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateTransferRequest(
        @NotNull
        UUID fromAccountId,

        @NotNull
        UUID toAccountId,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @Positive
        long amountMinor
) {
}
