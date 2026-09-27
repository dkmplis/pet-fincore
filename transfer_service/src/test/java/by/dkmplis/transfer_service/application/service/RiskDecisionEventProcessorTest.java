package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.application.exception.LedgerCallUncertainException;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.infrastructure.inbox.persistence.InboxEvent;
import by.dkmplis.transfer_service.infrastructure.inbox.persistence.InboxEventRepository;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RiskDecisionPayload;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiskDecisionEventProcessorTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private RiskDecisionEventProcessor processor;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @MockitoBean
    private LedgerClient ledgerClient;

    @Test
    void shouldProcessApprovedRiskDecision() {
        CreateTransferResult transfer =
                transferService.create(command());

        UUID ledgerTransactionId =
                UUID.randomUUID();

        when(ledgerClient.postTransfer(any()))
                .thenReturn(
                        new LedgerTransferResult(
                                ledgerTransactionId,
                                false
                        )
                );

        EventEnvelope<RiskDecisionPayload> event =
                approvedEvent(
                        transfer.transferId()
                );

        processor.process(
                event,
                "{}"
        );

        assertThat(
                transferRepository
                        .findById(transfer.transferId())
                        .orElseThrow()
                        .getState()
        )
                .isEqualTo(
                        TransferState.COMPLETED
                );

        InboxEvent inbox =
                inboxEventRepository
                        .findById(event.eventId())
                        .orElseThrow();

        assertThat(inbox.getProcessedAt())
                .isNotNull();

        verify(ledgerClient)
                .postTransfer(any());
    }

    @Test
    void shouldRejectWithoutCallingLedger() {
        CreateTransferResult transfer =
                transferService.create(command());

        EventEnvelope<RiskDecisionPayload> event =
                rejectedEvent(
                        transfer.transferId()
                );

        processor.process(
                event,
                "{}"
        );

        assertThat(
                transferRepository
                        .findById(transfer.transferId())
                        .orElseThrow()
                        .getState()
        )
                .isEqualTo(
                        TransferState.REJECTED
                );

        verifyNoInteractions(
                ledgerClient
        );
    }

    @Test
    void shouldRetryApprovedEventAfterUncertainLedgerCall() {
        CreateTransferResult transfer =
                transferService.create(command());

        EventEnvelope<RiskDecisionPayload> event =
                approvedEvent(
                        transfer.transferId()
                );

        when(ledgerClient.postTransfer(any()))
                .thenThrow(
                        new LedgerCallUncertainException(
                                "Ledger timeout"
                        )
                );

        assertThatThrownBy(
                () -> processor.process(
                        event,
                        "{}"
                )
        )
                .isInstanceOf(
                        LedgerCallUncertainException.class
                );

        assertThat(
                transferRepository
                        .findById(transfer.transferId())
                        .orElseThrow()
                        .getState()
        )
                .isEqualTo(
                        TransferState.RISK_APPROVED
                );

        InboxEvent afterFailure =
                inboxEventRepository
                        .findById(event.eventId())
                        .orElseThrow();

        assertThat(
                afterFailure.getProcessedAt()
        )
                .isNull();

        UUID ledgerTransactionId =
                UUID.randomUUID();

        reset(ledgerClient);

        when(ledgerClient.postTransfer(any()))
                .thenReturn(
                        new LedgerTransferResult(
                                ledgerTransactionId,
                                true
                        )
                );

        processor.process(
                event,
                "{}"
        );

        assertThat(
                transferRepository
                        .findById(transfer.transferId())
                        .orElseThrow()
                        .getState()
        )
                .isEqualTo(
                        TransferState.COMPLETED
                );

        InboxEvent afterRetry =
                inboxEventRepository
                        .findById(event.eventId())
                        .orElseThrow();

        assertThat(
                afterRetry.getProcessedAt()
        )
                .isNotNull();
    }

    private EventEnvelope<RiskDecisionPayload> approvedEvent(
            UUID transferId
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                "risk.approved",
                1,
                transferId,
                Instant.now(),
                new RiskDecisionPayload(
                        UUID.randomUUID(),
                        "RISK_CHECK_PASSED"
                )
        );
    }

    private EventEnvelope<RiskDecisionPayload> rejectedEvent(
            UUID transferId
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                "risk.rejected",
                1,
                transferId,
                Instant.now(),
                new RiskDecisionPayload(
                        UUID.randomUUID(),
                        "TRANSFER_AMOUNT_EXCEEDS_LIMIT"
                )
        );
    }

    private CreateTransferCommand command() {
        return new CreateTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BYN",
                10_000L
        );
    }
}