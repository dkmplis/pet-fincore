package by.dkmplis.transfer_service.api.dto;

import by.dkmplis.transfer_service.domain.enums.TransferState;

import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        TransferState state,
        boolean replayed
) {
}
