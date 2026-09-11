package by.dkmplis.transfer_service.infrastructure.outbox.exception;

import java.util.UUID;

public class OutboxClaimLostException
        extends RuntimeException {

    public OutboxClaimLostException(
            UUID eventId
    ) {
        super(
                "Outbox claim was lost for event %s"
                        .formatted(eventId)
        );
    }
}
