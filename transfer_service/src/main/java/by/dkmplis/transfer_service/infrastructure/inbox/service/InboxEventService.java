package by.dkmplis.transfer_service.infrastructure.inbox.service;

import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.infrastructure.inbox.persistence.InboxEvent;
import by.dkmplis.transfer_service.infrastructure.inbox.persistence.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxEventService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public InboxRegistration register(
            EventEnvelope<?> event,
            String rawEventJson
    ) {
        int registered = inboxEventRepository.register(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateId(),
                rawEventJson
        );

        InboxEvent inboxEvent = inboxEventRepository
                .findById(event.eventId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Inbox event was not registered"
                        )
                );

        if (registered == 0) {
            validateDuplicate(
                    inboxEvent,
                    event,
                    rawEventJson
            );
        }


        return new InboxRegistration(
                registered == 1,
                inboxEvent.getProcessedAt() != null
        );
    }

    @Transactional
    public void markProcessed(UUID eventId) {
        int updated = inboxEventRepository.markProcessed(
                eventId,
                Instant.now()
        );

        if (updated == 1) {
            return;
        }

        InboxEvent existing = inboxEventRepository
                .findById(eventId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Inbox event not found: " + eventId
                        )
                );

        if (existing.getProcessedAt() != null) {
            return;
        }

        throw new IllegalStateException(
                "Failed to mark inbox event as processed"
        );
    }

    private void validateDuplicate(
            InboxEvent existing,
            EventEnvelope<?> incoming,
            String rawEventJson
    ) {
        boolean sameEvent =
                existing.getEventType()
                        .equals(incoming.eventType())
                        && existing.getEventVersion()
                        == incoming.eventVersion()
                        && existing.getAggregateId()
                        .equals(incoming.aggregateId())
                        && existing.getPayload()
                        .equals(rawEventJson);

        if (!sameEvent) {
            throw new IllegalStateException(
                    "Inbox event id collision: "
                            + incoming.eventId()
            );
        }
    }
}
