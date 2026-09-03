package by.dkmplis.transfer_service.application.command;

import by.dkmplis.transfer_service.domain.enums.TransferState;

import java.util.UUID;

public record CreateTransferResult(
        UUID transferId,
        TransferState state,
        boolean replayed
) {
}
