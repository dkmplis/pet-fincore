package by.dkmplis.transfer_service.infrastructure.outbox.publisher;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID id,
        UUID claimToken,
        String topic,
        String key,
        String payload
) {
}
