package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.exception.LedgerCallUncertainException;
import by.dkmplis.transfer_service.application.exception.LedgerTransferRejectedException;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.application.port.LedgerTransferCommand;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class CreateTransferUseCaseTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private CreateTransferUseCase useCase;

    @MockitoBean
    private LedgerClient ledgerClient;

    @Test
    void shouldCompleteTransferWhenLedgerPostsTransaction() {
        UUID ledgerTransactionId = UUID.randomUUID();

        when(ledgerClient.postTransfer(any()))
                .thenReturn(
                        new LedgerTransferResult(
                                ledgerTransactionId,
                                false
                        )
                );

        CreateTransferResult result =
                useCase.execute(
                        command(100_00L)
                );

        assertThat(result.state())
                .isEqualTo(TransferState.COMPLETED);

        Transfer transfer = transferRepository
                .findById(result.transferId())
                .orElseThrow();

        assertThat(transfer.getState())
                .isEqualTo(TransferState.COMPLETED);

        assertThat(transfer.getLedgerTransactionId())
                .isEqualTo(ledgerTransactionId);

        verify(ledgerClient, times(1))
                .postTransfer(any());
    }

    @Test
    void shouldRejectTransferWhenLedgerRejectsOperation() {
        when(ledgerClient.postTransfer(any()))
                .thenThrow(
                        new LedgerTransferRejectedException(
                                "Insufficient funds"
                        )
                );

        CreateTransferResult result =
                useCase.execute(command(100_00L));

        assertThat(result.state())
                .isEqualTo(TransferState.REJECTED);

        Transfer transfer = transferRepository
                .findById(result.transferId())
                .orElseThrow();

        assertThat(transfer.getState())
                .isEqualTo(TransferState.REJECTED);

        assertThat(transfer.getLedgerTransactionId())
                .isNull();
    }

    @Test
    void shouldKeepTransferPendingWhenLedgerResultIsUncertain() {
        when(ledgerClient.postTransfer(any()))
                .thenThrow(
                        new LedgerCallUncertainException(
                                "Ledger timeout"
                        )
                );

        CreateTransferResult result =
                useCase.execute(command(100_00L));

        assertThat(result.state())
                .isEqualTo(TransferState.PENDING);

        Transfer transfer = transferRepository
                .findById(result.transferId())
                .orElseThrow();

        assertThat(transfer.getState())
                .isEqualTo(TransferState.PENDING);

        assertThat(transfer.getLedgerTransactionId())
                .isNull();
    }

    @Test
    void shouldRetryPendingTransferWithSameLedgerOperationId() {
        UUID externalOperationId = UUID.randomUUID();

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "BYN",
                        100_00L
                );

        UUID ledgerTransactionId = UUID.randomUUID();

        when(ledgerClient.postTransfer(any()))
                .thenThrow(
                        new LedgerCallUncertainException(
                                "Ledger timeout"
                        )
                )
                .thenReturn(
                        new LedgerTransferResult(
                                ledgerTransactionId,
                                true
                        )
                );

        CreateTransferResult first =
                useCase.execute(command);

        assertThat(first.state())
                .isEqualTo(TransferState.PENDING);

        CreateTransferResult second =
                useCase.execute(command);

        assertThat(second.transferId())
                .isEqualTo(first.transferId());

        assertThat(second.replayed()).isTrue();
        assertThat(second.state())
                .isEqualTo(TransferState.COMPLETED);

        ArgumentCaptor<LedgerTransferCommand> captor =
                ArgumentCaptor.forClass(
                        LedgerTransferCommand.class
                );

        verify(ledgerClient, times(2))
                .postTransfer(captor.capture());

        assertThat(
                captor.getAllValues()
                        .get(0)
                        .ledgerOperationId()
        ).isEqualTo(
                captor.getAllValues()
                        .get(1)
                        .ledgerOperationId()
        );
    }

    private CreateTransferCommand command(long amountMinor) {
        return new CreateTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BYN",
                amountMinor
        );
    }


}
