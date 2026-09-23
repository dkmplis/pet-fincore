package by.dkmplis.riskservice.infrastructure.outbox.publisher;

import by.dkmplis.riskservice.infrastructure.outbox.config.OutboxPublisherProperties;
import by.dkmplis.riskservice.infrastructure.outbox.service.OutboxClaimService;
import by.dkmplis.riskservice.infrastructure.outbox.service.OutboxStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxPublisherTest {

    @Mock
    private OutboxClaimService claimService;

    @Mock
    private OutboxStateService stateService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        OutboxPublisherProperties properties =
                new OutboxPublisherProperties(
                        10,
                        Duration.ofMinutes(3),
                        Duration.ofMillis(50)
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
    void shouldDoNothingWhenBatchIsEmpty() {
        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(List.of());

        publisher.publishBatch();

        verifyNoInteractions(
                kafkaTemplate,
                stateService
        );
    }

    @Test
    void shouldSendEventAndMarkPublishedAfterKafkaAck() {
        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(
                        List.of(event)
                );

        SendResult<String, String> sendResult =
                mock(SendResult.class);

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        )
                .thenReturn(
                        CompletableFuture.completedFuture(
                                sendResult
                        )
                );

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

        verify(
                stateService,
                never()
        ).markFailed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldMarkFailedWhenKafkaSendCompletesExceptionally() {
        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(
                        List.of(event)
                );

        CompletableFuture<SendResult<String, String>>
                future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new IllegalStateException(
                        "broker unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        )
                .thenReturn(future);

        publisher.publishBatch();

        verify(stateService)
                .markFailed(
                        event.id(),
                        event.claimToken(),
                        "broker unavailable"
                );

        verify(stateService, never())
                .markPublished(
                        event.id(),
                        event.claimToken()
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMarkFailedWhenKafkaSendTimesOut()
            throws Exception {

        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(
                        List.of(event)
                );

        CompletableFuture<SendResult<String, String>>
                future =
                mock(CompletableFuture.class);

        when(
                future.get(
                        50L,
                        TimeUnit.MILLISECONDS
                )
        )
                .thenThrow(
                        new TimeoutException()
                );

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        )
                .thenReturn(future);

        publisher.publishBatch();

        verify(stateService)
                .markFailed(
                        event.id(),
                        event.claimToken(),
                        "Kafka publishing timed out"
                );

        verify(stateService, never())
                .markPublished(
                        event.id(),
                        event.claimToken()
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRestoreInterruptAndStopProcessingCurrentBatch()
            throws Exception {

        ClaimedOutboxEvent first =
                event();

        ClaimedOutboxEvent second =
                event();

        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(
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
                        50L,
                        TimeUnit.MILLISECONDS
                )
        )
                .thenThrow(
                        new InterruptedException()
                );

        when(
                kafkaTemplate.send(
                        first.topic(),
                        first.key(),
                        first.payload()
                )
        )
                .thenReturn(future);

        try {
            publisher.publishBatch();

            assertThat(
                    Thread.currentThread()
                            .isInterrupted()
            )
                    .isTrue();

            verify(stateService)
                    .markFailed(
                            first.id(),
                            first.claimToken(),
                            "Kafka publishing was interrupted"
                    );

            verify(
                    kafkaTemplate,
                    never()
            )
                    .send(
                            second.topic(),
                            second.key(),
                            second.payload()
                    );
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void kafkaAckShouldNotBeConvertedToSendFailureWhenDbUpdateFails() {
        ClaimedOutboxEvent event =
                event();

        when(
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(3)
                )
        )
                .thenReturn(
                        List.of(event)
                );

        SendResult<String, String> sendResult =
                mock(SendResult.class);

        when(
                kafkaTemplate.send(
                        event.topic(),
                        event.key(),
                        event.payload()
                )
        )
                .thenReturn(
                        CompletableFuture.completedFuture(
                                sendResult
                        )
                );

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
        )
                .markFailed(
                        event.id(),
                        event.claimToken(),
                        "database unavailable"
                );
    }

    private ClaimedOutboxEvent event() {
        UUID transferId =
                UUID.randomUUID();

        return new ClaimedOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "risk.events.v1",
                transferId.toString(),
                """
                {
                  "eventType": "risk.approved"
                }
                """
        );
    }
}