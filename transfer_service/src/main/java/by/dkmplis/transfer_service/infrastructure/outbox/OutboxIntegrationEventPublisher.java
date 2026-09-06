package by.dkmplis.transfer_service.infrastructure.outbox;

import by.dkmplis.transfer_service.application.event.IntegrationEvent;
import by.dkmplis.transfer_service.application.port.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OutboxIntegrationEventPublisher
        implements IntegrationEventPublisher {

    private static final String AGGREGATE_TYPE =
            "TRANSFER";

    private static final String TOPIC =
            "transfers.events.v1";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(IntegrationEvent event) {
        String payload;

        try {
            payload =
                    objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize integration event",
                    exception
            );
        }

        OutboxEvent outboxEvent =
                new OutboxEvent(
                        event.eventId(),
                        AGGREGATE_TYPE,
                        event.aggregateId(),
                        event.eventType(),
                        event.eventVersion(),
                        TOPIC,
                        payload,
                        event.occurredAt()
                );

        repository.save(outboxEvent);
    }
}


