package by.dkmplis.riskservice.infrastructure.outbox.publisher;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID id,
        UUID claimToken,
        String topic,
        String key,
        String payload
) {
}
