package by.dkmplis.ledgerservice.application.exception;

import java.util.UUID;

public class IdempotencyConflictException
        extends IllegalStateException {

    public IdempotencyConflictException(UUID operationId) {
        super(
                "External operation ID was reused with different payload: %s"
                        .formatted(operationId)
        );
    }
}
