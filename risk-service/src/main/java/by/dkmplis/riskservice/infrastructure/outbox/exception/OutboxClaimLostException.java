package by.dkmplis.riskservice.infrastructure.outbox.exception;

import java.util.UUID;

public class OutboxClaimLostException
        extends RuntimeException {
    public OutboxClaimLostException(UUID outboxId) {
        super(
                "Outbox claim was lost for row %s"
                        .formatted(outboxId)
        );
    }
}
