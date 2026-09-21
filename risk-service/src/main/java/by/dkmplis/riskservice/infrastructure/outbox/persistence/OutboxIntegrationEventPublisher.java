package by.dkmplis.riskservice.infrastructure.outbox.persistence;

import by.dkmplis.riskservice.application.event.EventEnvelope;
import by.dkmplis.riskservice.application.event.IntegrationEvent;
import by.dkmplis.riskservice.application.port.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final String TOPIC = "risk.events.v1";

    private final OutboxEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(
            IntegrationEvent<?> event
    ) {
        EventEnvelope<?> eventEnvelope = EventEnvelope.from(event);

        String serializedEnvelope = serialize(eventEnvelope);

        OutboxEvent outboxEvent =
                new OutboxEvent(
                        UUID.randomUUID(),
                        event.eventId(),
                        TOPIC,
                        event.aggregateId().toString(),
                        event.eventType(),
                        event.eventVersion(),
                        event.aggregateId(),
                        serializedEnvelope,
                        event.occurredAt()
                );

        eventRepository.save(outboxEvent);
    }

    private String serialize(
            EventEnvelope<?> eventEnvelope
    ) {
        try {
            return objectMapper.writeValueAsString(
                    eventEnvelope
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize integration event",
                    exception
            );
        }
    }
}
