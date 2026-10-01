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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

    @Test
    void shouldMarkFailedWhenKafkaSendTimesOut()
            throws Exception {

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
                mock(CompletableFuture.class);

        when(
                future.get(
                        5000L,
                        TimeUnit.MILLISECONDS
                )
        ).thenThrow(
                new TimeoutException()
        );

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        ).thenReturn(future);

        publisher.publishBatch();

        verify(stateService)
                .markFailed(
                        event.id(),
                        event.claimToken(),
                        "Kafka publishing timed out"
                );

        verify(
                stateService,
                never()
        ).markPublished(
                event.id(),
                event.claimToken()
        );
    }

    @Test
    void shouldRestoreInterruptAndStopCurrentBatch()
            throws Exception {

        ClaimedOutboxEvent first =
                event();

        ClaimedOutboxEvent second =
                event();

        when(
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        CompletableFuture<SendResult<String, String>>
                future =
                mock(CompletableFuture.class);

        when(
                future.get(
                        5000L,
                        TimeUnit.MILLISECONDS
                )
        ).thenThrow(
                new InterruptedException()
        );

        when(
                kafkaTemplate.send(
                        first.topic(),
                        first.key(),
                        first.payload()
                )
        ).thenReturn(future);

        try {
            publisher.publishBatch();

            assertThat(
                    Thread.currentThread()
                            .isInterrupted()
            ).isTrue();

            verify(stateService)
                    .markFailed(
                            first.id(),
                            first.claimToken(),
                            "Kafka publishing was interrupted"
                    );

            verify(
                    kafkaTemplate,
                    never()
            ).send(
                    second.topic(),
                    second.key(),
                    second.payload()
            );

        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldNotMarkFailedWhenKafkaAckSucceededButDbUpdateFailed() {
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

        doThrow(
                new IllegalStateException(
                        "database unavailable"
                )
        )
                .when(stateService)
                .markPublished(
                        event.id(),
                        event.claimToken()
                );

        publisher.publishBatch();

        verify(stateService)
                .markPublished(
                        event.id(),
                        event.claimToken()
                );

        verify(
                stateService,
                never()
        ).markFailed(
                any(),
                any(),
                anyString()
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