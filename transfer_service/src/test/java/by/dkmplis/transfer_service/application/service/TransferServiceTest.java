package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.exception.TransferIdempotencyConflictException;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import by.dkmplis.transfer_service.support.ConcurrentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TransferServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Test
    void shouldCreatePendingTransfer() {
        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferResult result =
                transferService.create(
                        new CreateTransferCommand(
                                externalOperationId,
                                fromAccountId,
                                toAccountId,
                                "BYN",
                                100_00L
                        )
                );

        assertThat(result.replayed()).isFalse();
        assertThat(result.state())
                .isEqualTo(TransferState.PENDING);

        Transfer transfer = transferRepository
                .findById(result.transferId())
                .orElseThrow();

        assertThat(transfer.getExternalOperationId())
                .isEqualTo(externalOperationId);

        assertThat(transfer.getFromAccountId())
                .isEqualTo(fromAccountId);

        assertThat(transfer.getToAccountId())
                .isEqualTo(toAccountId);

        assertThat(transfer.getAmountMinor())
                .isEqualTo(100_00L);

        assertThat(transfer.getCurrency())
                .isEqualTo("BYN");

        assertThat(transfer.getState())
                .isEqualTo(TransferState.PENDING);

        assertThat(transfer.getLedgerOperationId())
                .isNotNull();

        assertThat(transfer.getLedgerTransactionId())
                .isNull();
    }

    @Test
    void shouldReturnExistingTransferForIdempotentReplay() {
        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        100_00L
                );

        CreateTransferResult first =
                transferService.create(command);

        CreateTransferResult second =
                transferService.create(command);

        assertThat(second.transferId())
                .isEqualTo(first.transferId());

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();

        assertThat(transferRepository.count())
                .isEqualTo(1);


        Transfer persisted = transferRepository
                .findById(first.transferId())
                .orElseThrow();

        UUID ledgerOperationId =
                persisted.getLedgerOperationId();

        CreateTransferResult replay =
                transferService.create(command);

        Transfer afterReplay = transferRepository
                .findById(replay.transferId())
                .orElseThrow();

        assertThat(afterReplay.getLedgerOperationId())
                .isEqualTo(ledgerOperationId);
    }

    @Test
    void shouldRejectSameOperationIdWithDifferentPayload() {
        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        transferService.create(
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        100_00L
                )
        );

        assertThatThrownBy(
                () -> transferService.create(
                        new CreateTransferCommand(
                                externalOperationId,
                                fromAccountId,
                                toAccountId,
                                "BYN",
                                200_00L
                        )
                )
        ).isInstanceOf(
                TransferIdempotencyConflictException.class
        );

        assertThat(transferRepository.count())
                .isEqualTo(1);
    }

    @Test
    void shouldHandleConcurrentIdempotentReplay()
            throws Exception {

        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        100_00L
                );

        List<CreateTransferResult> results =
                runConcurrently(
                        () -> transferService.create(command),
                        () -> transferService.create(command)
                );

        CreateTransferResult first = results.get(0);
        CreateTransferResult second = results.get(1);

        assertThat(first.transferId())
                .isEqualTo(second.transferId());

        assertThat(List.of(
                first.replayed(),
                second.replayed()
        ))
                .containsExactlyInAnyOrder(
                        false,
                        true
                );

        assertThat(transferRepository.count())
                .isEqualTo(1);

        Transfer persisted = transferRepository
                .findById(first.transferId())
                .orElseThrow();

        assertThat(persisted.getLedgerOperationId())
                .isNotNull();
    }

    @Test
    void shouldRejectConcurrentSameOperationIdWithDifferentPayload()
            throws Exception {

        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferCommand firstCommand =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        100_00L
                );

        CreateTransferCommand secondCommand =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        200_00L
                );

        List<ConcurrentResult<CreateTransferResult>> results =
                runConcurrently(
                        () -> capture(
                                () -> transferService.create(
                                        firstCommand
                                )
                        ),
                        () -> capture(
                                () -> transferService.create(
                                        secondCommand
                                )
                        )
                );

        long successCount = results.stream()
                .filter(result -> result.error() == null)
                .count();

        long conflictCount = results.stream()
                .filter(result ->
                        result.error()
                                instanceof TransferIdempotencyConflictException
                )
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        assertThat(transferRepository.count())
                .isEqualTo(1);
    }

    protected <T> ConcurrentResult<T> capture(
            Callable<T> action
    ) {
        try {
            return new ConcurrentResult<>(
                    action.call(),
                    null
            );
        } catch (Throwable throwable) {
            return new ConcurrentResult<>(
                    null,
                    throwable
            );
        }
    }



}
