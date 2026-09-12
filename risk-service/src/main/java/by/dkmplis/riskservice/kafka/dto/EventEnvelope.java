package by.dkmplis.riskservice.kafka.dto;

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
}
