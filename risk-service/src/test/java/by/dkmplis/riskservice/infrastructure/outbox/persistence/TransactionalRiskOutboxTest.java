package by.dkmplis.riskservice.infrastructure.outbox.persistence;

import by.dkmplis.riskservice.application.event.EventEnvelope;
import by.dkmplis.riskservice.application.service.RiskDecisionService;
import by.dkmplis.riskservice.application.service.TransferCreatedEventProcessor;
import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.infrastructure.kafka.dto.TransferCreatedPayload;
import by.dkmplis.riskservice.infrastructure.persistence.RiskAssessmentRepository;
import by.dkmplis.riskservice.support.AbstractRiskIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalRiskOutboxTest
        extends AbstractRiskIntegrationTest {

    @Autowired
    private TransferCreatedEventProcessor processor;

    @Autowired
    private RiskDecisionService riskDecisionService;

    @Autowired
    private RiskAssessmentRepository assessmentRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPersistApprovedRiskEvent()
            throws Exception {
        EventEnvelope<TransferCreatedPayload> event =
                event(10_000L);

        processor.process(
                event,
                rawPayload(event)
        );

        var assessment =
                assessmentRepository
                        .findByTransferId(
                                event.aggregateId()
                        )
                        .orElseThrow();

        var events =
                outboxRepository
                        .findAllByAggregateIdOrderByCreatedAtAsc(
                                event.aggregateId()
                        );

        assertThat(assessment.getStatus())
                .isEqualTo(RiskStatus.APPROVED);

        assertThat(events)
                .hasSize(1);

        OutboxEvent outboxEvent =
                events.getFirst();

        assertThat(outboxEvent.getEventId())
                .isNotNull();

        assertThat(outboxEvent.getTopic())
                .isEqualTo("risk.events.v1");

        assertThat(outboxEvent.getEventKey())
                .isEqualTo(
                        event.aggregateId().toString()
                );

        assertThat(outboxEvent.getEventType())
                .isEqualTo("risk.approved");

        assertThat(outboxEvent.getEventVersion())
                .isEqualTo(1);

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(event.aggregateId());

        assertThat(outboxEvent.getPublishedAt())
                .isNull();

        assertThat(outboxEvent.getAttempts())
                .isZero();

        assertThat(outboxEvent.getClaimedAt())
                .isNull();

        assertThat(outboxEvent.getClaimToken())
                .isNull();

        var json =
                objectMapper.readTree(
                        outboxEvent.getPayload()
                );

        assertThat(
                json.get("eventId").asString()
        ).isEqualTo(
                outboxEvent.getEventId().toString()
        );

        assertThat(
                json.get("eventType").asString()
        ).isEqualTo("risk.approved");

        assertThat(
                json.get("eventVersion").asInt()
        ).isEqualTo(1);

        assertThat(
                json.get("aggregateId").asString()
        ).isEqualTo(
                event.aggregateId().toString()
        );

        assertThat(
                json.get("occurredAt").asString()
        ).isNotBlank();

        var payload =
                json.get("payload");

        assertThat(
                payload.get("riskAssessmentId")
                        .asString()
        ).isEqualTo(
                assessment.getId().toString()
        );

        assertThat(
                payload.get("reason").asString()
        ).isEqualTo(
                "RISK_CHECK_PASSED"
        );
    }

    @Test
    void shouldPersistRejectedRiskEvent() {
        EventEnvelope<TransferCreatedPayload> event =
                event(500_001L);

        processor.process(
                event,
                rawPayload(event)
        );

        var assessment =
                assessmentRepository
                        .findByTransferId(
                                event.aggregateId()
                        )
                        .orElseThrow();

        var events =
                outboxRepository
                        .findAllByAggregateIdOrderByCreatedAtAsc(
                                event.aggregateId()
                        );

        assertThat(assessment.getStatus())
                .isEqualTo(RiskStatus.REJECTED);

        assertThat(events)
                .hasSize(1);

        assertThat(events.getFirst().getEventType())
                .isEqualTo("risk.rejected");

        assertThat(events.getFirst().getAggregateId())
                .isEqualTo(event.aggregateId());
    }

    @Test
    void shouldNotCreateDuplicateEventForExistingDecision() {
        EventEnvelope<TransferCreatedPayload> event =
                event(10_000L);

        processor.process(
                event,
                rawPayload(event)
        );

        var assessment =
                assessmentRepository
                        .findByTransferId(
                                event.aggregateId()
                        )
                        .orElseThrow();

        RiskStatus replayedStatus =
                riskDecisionService.decide(
                        assessment.getId()
                );

        assertThat(replayedStatus)
                .isEqualTo(RiskStatus.APPROVED);

        assertThat(
                outboxRepository
                        .findAllByAggregateIdOrderByCreatedAtAsc(
                                event.aggregateId()
                        )
        ).hasSize(1);
    }

    private EventEnvelope<TransferCreatedPayload> event(
            long amountMinor
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                "transfer.created",
                1,
                UUID.randomUUID(),
                Instant.now(),
                new TransferCreatedPayload(
                        UUID.fromString(
                                "00000000-0000-0000-0000-000000000001"
                        ),
                        UUID.fromString(
                                "00000000-0000-0000-0000-000000000002"
                        ),
                        "BYN",
                        amountMinor
                )
        );
    }

    private String rawPayload(
            EventEnvelope<TransferCreatedPayload> event
    ) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "transfer.created",
                  "eventVersion": 1,
                  "aggregateId": "%s"
                }
                """.formatted(
                event.eventId(),
                event.aggregateId()
        );
    }
}