package by.dkmplis.transfer_service.application.exception;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class TransferNotFoundException
        extends EntityNotFoundException {
    public TransferNotFoundException(UUID transferId) {
        super(
                "Transfer %s was not found"
                        .formatted(transferId)
        );
    }
}
