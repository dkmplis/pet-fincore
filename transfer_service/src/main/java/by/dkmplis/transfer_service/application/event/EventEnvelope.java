package by.dkmplis.transfer_service.application.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        T payload
) {
    public static <T> EventEnvelope<T> from(
            IntegrationEvent<T> event
    ) {
        return new EventEnvelope<>(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateId(),
                event.occurredAt(),
                event.payload()
        );
    }
}
