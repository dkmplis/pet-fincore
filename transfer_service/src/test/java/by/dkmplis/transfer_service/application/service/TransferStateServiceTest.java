package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferStateServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferStateService stateService;

    @Test
    void shouldPersistRiskApprovedState() {
        CreateTransferResult created =
                transferService.create(command());

        TransferState result =
                stateService.markRiskApproved(
                        created.transferId()
                );

        assertThat(result)
                .isEqualTo(TransferState.RISK_APPROVED);

        Transfer persisted = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertThat(persisted.getState())
                .isEqualTo(TransferState.RISK_APPROVED);
    }

    @Test
    void repeatedRiskApprovalShouldBeIdempotent() {
        CreateTransferResult created =
                transferService.create(command());

        stateService.markRiskApproved(
                created.transferId()
        );

        TransferState second =
                stateService.markRiskApproved(
                        created.transferId()
                );

        assertThat(second)
                .isEqualTo(TransferState.RISK_APPROVED);

        Transfer persisted = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertThat(persisted.getState())
                .isEqualTo(TransferState.RISK_APPROVED);
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