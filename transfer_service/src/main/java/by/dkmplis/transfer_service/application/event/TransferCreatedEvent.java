package by.dkmplis.transfer_service.application.event;

import java.time.Instant;
import java.util.UUID;

public record TransferCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt,
        TransferCreatedPayload payload
) implements IntegrationEvent<TransferCreatedPayload> {

    private static final String EVENT_TYPE =
            "transfer.created";

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
