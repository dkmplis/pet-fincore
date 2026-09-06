package by.dkmplis.transfer_service.application.port;

import by.dkmplis.transfer_service.application.event.IntegrationEvent;

public interface IntegrationEventPublisher {
    void publish(IntegrationEvent event);
}
