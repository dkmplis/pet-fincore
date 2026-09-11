package by.dkmplis.transfer_service.infrastructure.outbox.publisher;

import by.dkmplis.transfer_service.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.transfer_service.infrastructure.outbox.config.OutboxPublisherProperties;
import by.dkmplis.transfer_service.infrastructure.outbox.service.OutboxClaimService;
import by.dkmplis.transfer_service.infrastructure.outbox.service.OutboxStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOutboxPublisher {

    private final OutboxClaimService claimService;
    private final OutboxStateService stateService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;

    public void publishBatch() {
        List<ClaimedOutboxEvent> events =
                claimService.claimBatch(
                        properties.batchSize(),
                        properties.claimTimeout()
                );

        for (ClaimedOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(
            ClaimedOutboxEvent event
    ) {
        try {
            kafkaTemplate.send(
                            event.topic(),
                            event.key(),
                            event.payload()
                    )
                    .get(
                            properties.sendTimeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    );
            stateService.markPublished(
                    event.id(),
                    event.claimToken()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            markFailed(
                    event,
                    "Kafka publishing was interrupted"

            );
        } catch (Exception exception) {
            markFailed(
                    event,
                    exception.getMessage()
            );
        }
    }

    private void markFailed(
            ClaimedOutboxEvent event,
            String error
    ) {
        try {
            stateService.markFailed(
                    event.id(),
                    event.claimToken(),
                    error
            );
        } catch (OutboxClaimLostException exception) {
            log.warn(
                    "Cannot mark outbox event as failed because claim was lost: eventId={}",
                    event.id()
            );
        }
    }
}
