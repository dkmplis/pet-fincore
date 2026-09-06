package by.dkmplis.transfer_service.application.event;

import java.time.Instant;
import java.util.UUID;

public interface IntegrationEvent<T> {

    UUID eventId();
    UUID aggregateId();
    String eventType();
    int eventVersion();
    Instant occurredAt();
    T payload();
}
