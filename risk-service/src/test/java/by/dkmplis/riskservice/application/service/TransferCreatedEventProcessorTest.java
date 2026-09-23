package by.dkmplis.riskservice.application.service;

import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.infrastructure.persistence.RiskAssessmentRepository;
import by.dkmplis.riskservice.infrastructure.inbox.persistence.InboxEventRepository;
import by.dkmplis.riskservice.application.event.EventEnvelope;
import by.dkmplis.riskservice.infrastructure.kafka.dto.TransferCreatedPayload;
import by.dkmplis.riskservice.support.AbstractRiskIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferCreatedEventProcessorTest
        extends AbstractRiskIntegrationTest {

    @Autowired
    private TransferCreatedEventProcessor processor;

    @Autowired
    private InboxEventRepository inboxRepository;

    @Autowired
    private RiskAssessmentRepository riskRepository;

    @Test
    void shouldProcessNewTransferCreatedEvent() {
        EventEnvelope<TransferCreatedPayload> event =
                event(10_000L);

        processor.process(
                event,
                rawPayload(event)
        );

        assertThat(inboxRepository.count())
                .isEqualTo(1);

        var inbox =
                inboxRepository
                        .findById(event.eventId())
                        .orElseThrow();

        assertThat(inbox.getEventType())
                .isEqualTo("transfer.created");

        assertThat(inbox.getEventVersion())
                .isEqualTo(1);

        assertThat(inbox.getAggregateId())
                .isEqualTo(event.aggregateId());

        assertThat(inbox.getProcessedAt())
                .isNotNull();

        assertThat(riskRepository.count())
                .isEqualTo(1);

        var assessment =
                riskRepository
                        .findByTransferId(
                                event.aggregateId()
                        )
                        .orElseThrow();

        assertThat(assessment.getTransferId())
                .isEqualTo(event.aggregateId());

        assertThat(assessment.getFromAccountId())
                .isEqualTo(
                        event.payload().fromAccountId()
                );

        assertThat(assessment.getToAccountId())
                .isEqualTo(
                        event.payload().toAccountId()
                );

        assertThat(assessment.getCurrency())
                .isEqualTo("BYN");

        assertThat(assessment.getAmountMinor())
                .isEqualTo(10_000L);

        assertThat(assessment.getStatus())
                .isEqualTo(
                        RiskStatus.APPROVED
                );

        assertThat(assessment.getDecisionReason())
                .isEqualTo(
                        "RISK_CHECK_PASSED"
                );

        assertThat(assessment.getDecidedAt())
                .isNotNull();
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        EventEnvelope<TransferCreatedPayload> event =
                event(10_000L);

        String rawPayload =
                rawPayload(event);

        processor.process(
                event,
                rawPayload
        );

        processor.process(
                event,
                rawPayload
        );

        assertThat(inboxRepository.count())
                .isEqualTo(1);

        assertThat(riskRepository.count())
                .isEqualTo(1);

        assertThat(
                riskRepository.findByTransferId(
                        event.aggregateId()
                )
        ).isPresent();
    }

    @Test
    void shouldRollbackInboxWhenBusinessProcessingFails() {
        EventEnvelope<TransferCreatedPayload> event =
                event(0L);

        assertThatThrownBy(
                () -> processor.process(
                        event,
                        rawPayload(event)
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThat(inboxRepository.count())
                .isZero();

        assertThat(riskRepository.count())
                .isZero();

        assertThat(
                inboxRepository.findById(
                        event.eventId()
                )
        ).isEmpty();
    }

    private EventEnvelope<TransferCreatedPayload> event(
            long amountMinor
    ) {
        UUID transferId =
                UUID.randomUUID();

        return new EventEnvelope<>(
                UUID.randomUUID(),
                "transfer.created",
                1,
                transferId,
                Instant.now(),
                new TransferCreatedPayload(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
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