package by.dkmplis.riskservice.application.port;

import by.dkmplis.riskservice.application.event.IntegrationEvent;

public interface IntegrationEventPublisher {
    void publish(
            IntegrationEvent<?> event
    );
}
