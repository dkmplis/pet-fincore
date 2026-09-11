package by.dkmplis.transfer_service.infrastructure.outbox.publisher;

import by.dkmplis.transfer_service.infrastructure.outbox.config.OutboxPublisherProperties;
import by.dkmplis.transfer_service.infrastructure.outbox.service.OutboxClaimService;
import by.dkmplis.transfer_service.infrastructure.outbox.service.OutboxStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class KafkaOutboxPublisherTest {

    private OutboxClaimService claimService;
    private OutboxStateService stateService;
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        claimService =
                mock(OutboxClaimService.class);

        stateService =
                mock(OutboxStateService.class);

        kafkaTemplate =
                mock(KafkaTemplate.class);

        OutboxPublisherProperties properties =
                new OutboxPublisherProperties(
                        100,
                        Duration.ofMinutes(1),
                        Duration.ofSeconds(5)
                );

        publisher =
                new KafkaOutboxPublisher(
                        claimService,
                        stateService,
                        kafkaTemplate,
                        properties
                );
    }

    @Test
    void shouldPublishClaimedEventAndMarkItPublished() {
        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                List.of(event)
        );

        CompletableFuture<SendResult<String, String>>
                future =
                CompletableFuture.completedFuture(
                        null
                );

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        ).thenReturn(future);

        publisher.publishBatch();

        verify(kafkaTemplate)
                .send(
                        event.topic(),
                        event.key(),
                        event.payload()
                );

        verify(stateService)
                .markPublished(
                        event.id(),
                        event.claimToken()
                );

        verify(stateService, never())
                .markFailed(
                        any(),
                        any(),
                        anyString()
                );
    }

    @Test
    void shouldMarkEventFailedWhenKafkaPublishFails() {
        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                List.of(event)
        );

        CompletableFuture<SendResult<String, String>>
                future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException(
                        "Kafka unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        ).thenReturn(future);

        publisher.publishBatch();

        verify(stateService, never())
                .markPublished(
                        any(),
                        any()
                );

        verify(stateService)
                .markFailed(
                        eq(event.id()),
                        eq(event.claimToken()),
                        contains("Kafka unavailable")
                );
    }

    @Test
    void shouldDoNothingWhenNoEventsAreAvailable() {
        when(
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                List.of()
        );

        publisher.publishBatch();

        verifyNoInteractions(
                kafkaTemplate,
                stateService
        );
    }

    private ClaimedOutboxEvent event() {
        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "transfers.events.v1",
                UUID.randomUUID().toString(),
                """
                {
                  "eventType": "transfer.created"
                }
                """
        );
    }
}