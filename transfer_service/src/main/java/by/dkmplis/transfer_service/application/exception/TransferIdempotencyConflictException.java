package by.dkmplis.transfer_service.application.exception;

import java.util.UUID;

public class TransferIdempotencyConflictException extends RuntimeException {

    public TransferIdempotencyConflictException(
            UUID externalOperationId
    ) {
        super(
                "Idempotency key %s was already used with different request"
                        .formatted(externalOperationId)
        );
    }
}
