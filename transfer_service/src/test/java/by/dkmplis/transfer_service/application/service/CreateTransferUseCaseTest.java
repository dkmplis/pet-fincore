package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;


public class CreateTransferUseCaseTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private CreateTransferUseCase useCase;

    @MockitoBean
    private LedgerClient ledgerClient;

    @Test
    void shouldCreatePendingTransferWithoutCallingLedger() {
        CreateTransferResult result =
                useCase.execute(
                        command()
                );

        assertThat(result.state())
                .isEqualTo(TransferState.PENDING);

        assertThat(result.replayed())
                .isFalse();

        Transfer persisted =
                transferRepository
                        .findById(result.transferId())
                        .orElseThrow();

        assertThat(persisted.getState())
                .isEqualTo(TransferState.PENDING);

        assertThat(persisted.getLedgerTransactionId())
                .isNull();

        verifyNoInteractions(
                ledgerClient
        );
    }

    @Test
    void idempotentReplayShouldNotCallLedger() {
        CreateTransferCommand command =
                command();

        CreateTransferResult first =
                useCase.execute(command);

        CreateTransferResult replay =
                useCase.execute(command);

        assertThat(replay.transferId())
                .isEqualTo(first.transferId());

        assertThat(replay.state())
                .isEqualTo(TransferState.PENDING);

        assertThat(replay.replayed())
                .isTrue();

        verifyNoInteractions(
                ledgerClient
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
