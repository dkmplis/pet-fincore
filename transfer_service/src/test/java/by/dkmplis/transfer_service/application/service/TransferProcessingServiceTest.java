package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferProcessingServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferStateService stateService;

    @Autowired
    private TransferProcessingService processingService;

    @MockitoBean
    private LedgerClient ledgerClient;


    @Test
    void shouldNotCallLedgerForPendingTransfer() {
        CreateTransferResult created =
                transferService.create(command());

        TransferState result =
                processingService.process(
                        created.transferId()
                );

        assertThat(result)
                .isEqualTo(TransferState.PENDING);

        verifyNoInteractions(
                ledgerClient
        );
    }

    @Test
    void shouldProcessRiskApprovedTransfer() {
        CreateTransferResult created =
                transferService.create(command());

        stateService.markRiskApproved(
                created.transferId()
        );

        UUID ledgerTransactionId =
                UUID.randomUUID();

        when(ledgerClient.postTransfer(any()))
                .thenReturn(
                        new LedgerTransferResult(
                                ledgerTransactionId,
                                false
                        )
                );

        TransferState result =
                processingService.process(
                        created.transferId()
                );

        assertThat(result)
                .isEqualTo(TransferState.COMPLETED);

        Transfer persisted =
                transferRepository
                        .findById(created.transferId())
                        .orElseThrow();

        assertThat(persisted.getState())
                .isEqualTo(TransferState.COMPLETED);

        assertThat(
                persisted.getLedgerTransactionId()
        )
                .isEqualTo(ledgerTransactionId);

        verify(ledgerClient)
                .postTransfer(any());
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