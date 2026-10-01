package by.dkmplis.transfer_service.infrastructure.kafka.listener;

import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.application.service.RiskDecisionEventProcessor;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RiskDecisionPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RiskEventsKafkaListenerTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000001"
            );

    private static final UUID TRANSFER_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000002"
            );

    private static final UUID ASSESSMENT_ID =
            UUID.fromString(
                    "00000000-0000-0000-0000-000000000003"
            );

    private RiskDecisionEventProcessor processor;

    private RiskEventsKafkaListener listener;

    @BeforeEach
    void setUp() {
        processor =
                mock(
                        RiskDecisionEventProcessor.class
                );

        listener =
                new RiskEventsKafkaListener(
                        new ObjectMapper(),
                        processor
                );
    }

    @Test
    void shouldProcessRiskApprovedEvent() {
        String json = eventJson(
                "risk.approved",
                1
        );

        listener.consume(json);

        ArgumentCaptor<
                EventEnvelope<RiskDecisionPayload>
                > captor =
                ArgumentCaptor.forClass(
                        EventEnvelope.class
                );

        verify(processor)
                .process(
                        captor.capture(),
                        eq(json)
                );

        EventEnvelope<RiskDecisionPayload> event =
                captor.getValue();

        assertThat(event.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(event.eventType())
                .isEqualTo("risk.approved");

        assertThat(event.eventVersion())
                .isEqualTo(1);

        assertThat(event.aggregateId())
                .isEqualTo(TRANSFER_ID);

        assertThat(
                event.payload()
                        .riskAssessmentId()
        )
                .isEqualTo(ASSESSMENT_ID);

        assertThat(
                event.payload().reason()
        )
                .isEqualTo(
                        "RISK_CHECK_PASSED"
                );
    }

    @Test
    void shouldProcessRiskRejectedEvent() {
        String json = eventJson(
                "risk.rejected",
                1
        );

        listener.consume(json);

        ArgumentCaptor<
                EventEnvelope<RiskDecisionPayload>
                > captor =
                ArgumentCaptor.forClass(
                        EventEnvelope.class
                );

        verify(processor)
                .process(
                        captor.capture(),
                        eq(json)
                );

        assertThat(
                captor.getValue().eventType()
        )
                .isEqualTo(
                        "risk.rejected"
                );
    }

    @Test
    void shouldIgnoreUnsupportedEventType() {
        String json = eventJson(
                "risk.unknown",
                1
        );

        listener.consume(json);

        verifyNoInteractions(
                processor
        );
    }

    @Test
    void shouldRejectUnsupportedVersion() {
        String json = eventJson(
                "risk.approved",
                2
        );

        assertThatThrownBy(
                () -> listener.consume(json)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Unsupported risk.approved"
                );

        verifyNoInteractions(
                processor
        );
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

        verifyNoInteractions(
                processor
        );
    }

    private String eventJson(
            String eventType,
            int eventVersion
    ) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "eventVersion": %d,
                  "aggregateId": "%s",
                  "occurredAt": null,
                  "payload": {
                    "riskAssessmentId": "%s",
                    "reason": "RISK_CHECK_PASSED"
                  }
                }
                """.formatted(
                EVENT_ID,
                eventType,
                eventVersion,
                TRANSFER_ID,
                ASSESSMENT_ID
        );
    }
}