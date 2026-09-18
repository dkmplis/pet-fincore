package by.dkmplis.riskservice.kafka.listener;

import by.dkmplis.riskservice.application.service.TransferCreatedEventProcessor;
import by.dkmplis.riskservice.infrastructure.kafka.dto.EventEnvelope;
import by.dkmplis.riskservice.infrastructure.kafka.dto.TransferCreatedPayload;
import by.dkmplis.riskservice.infrastructure.kafka.listener.TransferEventsKafkaListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TransferEventsKafkaListenerTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000001"
            );

    private static final UUID TRANSFER_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000002"
            );

    private static final UUID FROM_ACCOUNT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000003"
            );

    private static final UUID TO_ACCOUNT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000004"
            );

    private TransferCreatedEventProcessor processor;

    private TransferEventsKafkaListener listener;

    @BeforeEach
    void setUp() {
        processor =
                mock(
                        TransferCreatedEventProcessor.class
                );

        listener =
                new TransferEventsKafkaListener(
                        new ObjectMapper(),
                        processor
                );
    }

    @Test
    void shouldProcessTransferCreatedEvent() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "transfer.created",
                  "eventVersion": 1,
                  "aggregateId": "%s",
                  "occurredAt": null,
                  "payload": {
                    "fromAccountId": "%s",
                    "toAccountId": "%s",
                    "currency": "BYN",
                    "amountMinor": 10000
                  }
                }
                """.formatted(
                EVENT_ID,
                TRANSFER_ID,
                FROM_ACCOUNT_ID,
                TO_ACCOUNT_ID
        );

        listener.consume(json);

        ArgumentCaptor<
                EventEnvelope<TransferCreatedPayload>
                > eventCaptor =
                ArgumentCaptor.forClass(
                        EventEnvelope.class
                );

        verify(processor)
                .process(
                        eventCaptor.capture(),
                        eq(json)
                );

        EventEnvelope<TransferCreatedPayload> event =
                eventCaptor.getValue();

        assertThat(event.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(event.eventType())
                .isEqualTo("transfer.created");

        assertThat(event.eventVersion())
                .isEqualTo(1);

        assertThat(event.aggregateId())
                .isEqualTo(TRANSFER_ID);

        assertThat(event.payload().fromAccountId())
                .isEqualTo(FROM_ACCOUNT_ID);

        assertThat(event.payload().toAccountId())
                .isEqualTo(TO_ACCOUNT_ID);

        assertThat(event.payload().currency())
                .isEqualTo("BYN");

        assertThat(event.payload().amountMinor())
                .isEqualTo(10_000L);
    }

    @Test
    void shouldIgnoreUnsupportedEventType() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "transfer.completed",
                  "eventVersion": 1,
                  "aggregateId": "%s",
                  "occurredAt": null,
                  "payload": {
                    "ledgerTransactionId":
                      "00000000-0000-0000-0000-000000000005"
                  }
                }
                """.formatted(
                EVENT_ID,
                TRANSFER_ID
        );

        listener.consume(json);

        verifyNoInteractions(processor);
    }

    @Test
    void shouldRejectUnsupportedTransferCreatedVersion() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "transfer.created",
                  "eventVersion": 2,
                  "aggregateId": "%s",
                  "occurredAt": null,
                  "payload": {
                    "fromAccountId": "%s",
                    "toAccountId": "%s",
                    "currency": "BYN",
                    "amountMinor": 10000
                  }
                }
                """.formatted(
                EVENT_ID,
                TRANSFER_ID,
                FROM_ACCOUNT_ID,
                TO_ACCOUNT_ID
        );

        assertThatThrownBy(
                () -> listener.consume(json)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Unsupported transfer.created"
                );

        verifyNoInteractions(processor);
    }

    @Test
    void shouldRejectMalformedJson() {
        String json = """
                {
                  "eventId":
                }
                """;

        assertThatThrownBy(
                () -> listener.consume(json)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Failed to deserialize event envelope"
                );

        verifyNoInteractions(processor);
    }

    @Test
    void shouldRejectInvalidTransferCreatedPayload() {
        String json = """
            {
              "eventId": "%s",
              "eventType": "transfer.created",
              "eventVersion": 1,
              "aggregateId": "%s",
              "occurredAt": null,
              "payload": "not-an-object"
            }
            """.formatted(
                EVENT_ID,
                TRANSFER_ID
        );

        assertThatThrownBy(
                () -> listener.consume(json)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Failed to deserialize transfer.created payload"
                );

        verifyNoInteractions(processor);
    }
}