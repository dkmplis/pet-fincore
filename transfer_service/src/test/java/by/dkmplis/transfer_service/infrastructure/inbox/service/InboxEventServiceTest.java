package by.dkmplis.transfer_service.infrastructure.inbox.service;

import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RiskDecisionPayload;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboxEventServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private InboxEventService inboxEventService;

    @Test
    void shouldRejectDifferentEventWithSameEventId() {
        UUID eventId =
                UUID.randomUUID();

        UUID transferId =
                UUID.randomUUID();

        EventEnvelope<RiskDecisionPayload> first =
                new EventEnvelope<>(
                        eventId,
                        "risk.approved",
                        1,
                        transferId,
                        Instant.now(),
                        new RiskDecisionPayload(
                                UUID.randomUUID(),
                                "RISK_CHECK_PASSED"
                        )
                );

        inboxEventService.register(
                first,
                """
                {
                  "eventType": "risk.approved"
                }
                """
        );

        EventEnvelope<RiskDecisionPayload> conflicting =
                new EventEnvelope<>(
                        eventId,
                        "risk.rejected",
                        1,
                        transferId,
                        Instant.now(),
                        new RiskDecisionPayload(
                                UUID.randomUUID(),
                                "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                        )
                );

        assertThatThrownBy(
                () -> inboxEventService.register(
                        conflicting,
                        """
                        {
                          "eventType": "risk.rejected"
                        }
                        """
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Inbox event id collision: "
                                + eventId
                );
    }

    @Test
    void shouldAcceptExactDuplicateEvent() {
        UUID eventId =
                UUID.randomUUID();

        EventEnvelope<RiskDecisionPayload> event =
                new EventEnvelope<>(
                        eventId,
                        "risk.approved",
                        1,
                        UUID.randomUUID(),
                        Instant.now(),
                        new RiskDecisionPayload(
                                UUID.randomUUID(),
                                "RISK_CHECK_PASSED"
                        )
                );

        String rawJson =
                """
                {
                  "eventType": "risk.approved"
                }
                """;

        InboxRegistration first =
                inboxEventService.register(
                        event,
                        rawJson
                );

        InboxRegistration duplicate =
                inboxEventService.register(
                        event,
                        rawJson
                );

        org.assertj.core.api.Assertions
                .assertThat(first.newlyRegistered())
                .isTrue();

        org.assertj.core.api.Assertions
                .assertThat(duplicate.newlyRegistered())
                .isFalse();
    }
}