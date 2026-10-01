package by.dkmplis.transfer_service.infrastructure.kafka.dto;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record RawEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        Instant occurredAt,
        JsonNode payload

) {
}
