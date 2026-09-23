package by.dkmplis.riskservice.infrastructure.outbox.publisher;

import by.dkmplis.riskservice.infrastructure.outbox.config.OutboxPublisherProperties;
import by.dkmplis.riskservice.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.riskservice.infrastructure.outbox.service.OutboxClaimService;
import by.dkmplis.riskservice.infrastructure.outbox.service.OutboxStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            boolean continueBatch = publish(event);

            if (!continueBatch) {
                return;
            }
        }
    }

    private boolean publish(
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

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            markFailed(
                    event,
                    "Kafka publishing was interrupted"
            );

            return false;

        } catch (TimeoutException exception) {
            log.warn(
                    "Kafka send timed out; delivery status is uncertain and event may be retried: outboxId={}",
                    event.id()
            );

            markFailed(
                    event,
                    "Kafka publishing timed out"
            );

            return true;

        } catch (ExecutionException exception) {
            markFailed(
                    event,
                    errorMessage(
                            exception.getCause() != null
                                    ? exception.getCause()
                                    : exception
                    )
            );

            return true;

        } catch (RuntimeException exception) {
            markFailed(
                    event,
                    errorMessage(exception)
            );

            return true;
        }

        markPublishedAfterKafkaAck(event);

        return true;
    }

    private void markPublishedAfterKafkaAck(
            ClaimedOutboxEvent event
    ) {
        try {
            stateService.markPublished(
                    event.id(),
                    event.claimToken()
            );

        } catch (OutboxClaimLostException exception) {
            log.warn(
                    "Kafka acknowledged outbox event, but its claim was already lost: outboxId={}",
                    event.id()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Kafka acknowledged outbox event, but database state could not be marked as published. "
                            + "The event may be delivered again after claim recovery: outboxId={}",
                    event.id(),
                    exception
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
                    "Cannot mark outbox event as failed because claim was lost: outboxId={}",
                    event.id()
            );
        }
    }

    private String errorMessage(
            Throwable throwable
    ) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }
}