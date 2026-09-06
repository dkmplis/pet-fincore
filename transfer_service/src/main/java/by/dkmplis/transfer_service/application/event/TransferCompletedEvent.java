package by.dkmplis.transfer_service.application.event;

import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID ledgerTransactionId,
        Instant occurredAt
) implements IntegrationEvent {

    private static final String EVENT_TYPE =
            "transfer.completed";

    private static final int EVENT_VERSION = 1;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public int eventVersion() {
        return EVENT_VERSION;
    }
}
