package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.exception.IdempotencyConflictException;
import by.dkmplis.ledgerservice.application.exception.InsufficientFundsException;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.TransactionState;
import by.dkmplis.ledgerservice.support.AbstractLedgerIntegrationTest;
import by.dkmplis.ledgerservice.support.CommandFactory;
import by.dkmplis.ledgerservice.support.ConcurrentResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LedgerPostingServiceTest extends AbstractLedgerIntegrationTest {

    @Test
    void shouldPostFundingAndTransfer() {
        fundAlice(100_00L);

        assertThat(balance(aliceId)).isEqualTo(100_00L);

        UUID operationId = UUID.randomUUID();

        PostLedgerTransactionCommand transfer = CommandFactory.twoLegCommand(
                operationId,
                aliceId,
                bobId,
                25_00L,
                LedgerTransactionType.TRANSFER
        );

        PostLedgerTransactionResult result =
                postingService.post(transfer);

        assertThat(result.replayed()).isFalse();
        assertThat(balance(aliceId)).isEqualTo(75_00L);
        assertThat(balance(bobId)).isEqualTo(25_00L);

        var transaction = transactionRepository.findById(result.transactionId())
                .orElseThrow();

        assertThat(transaction.getState()).isEqualTo(TransactionState.POSTED);
        assertThat(
                entryRepository.findAllByTransactionId(result.transactionId())
        ).hasSize(2);

    }

    @Test
    void shouldReturnExistingTransactionForIdempotentReplay() {

        UUID operationId = UUID.randomUUID();

        PostLedgerTransactionCommand command =
                CommandFactory.twoLegCommand(
                        operationId,
                        settlementId,
                        aliceId,
                        100_00L,
                        LedgerTransactionType.FUNDING
                );

        PostLedgerTransactionResult first =
                postingService.post(command);

        PostLedgerTransactionResult second =
                postingService.post(command);

        assertThat(first.replayed())
                .isFalse();

        assertThat(second.replayed())
                .isTrue();

        assertThat(second.transactionId())
                .isEqualTo(first.transactionId());

        assertThat(transactionRepository.count())
                .isEqualTo(1);

        assertThat(entryRepository.count())
                .isEqualTo(2);

        assertThat(balance(aliceId))
                .isEqualTo(100_00L);
    }

    @Test
    void shouldRejectSameOperationIdWithDifferentPayload() {
        UUID operationId = UUID.randomUUID();

        postingService.post(
                CommandFactory.twoLegCommand(
                        operationId,
                        settlementId,
                        aliceId,
                        100_00L,
                        LedgerTransactionType.FUNDING)
        );

        PostLedgerTransactionCommand conflicting =
                CommandFactory.twoLegCommand(
                        operationId,
                        settlementId,
                        aliceId,
                        200_00L,
                        LedgerTransactionType.FUNDING
                );

        assertThatThrownBy(
                () -> postingService.post(conflicting)
        ).isInstanceOf(IdempotencyConflictException.class);

        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(balance(aliceId)).isEqualTo(100_00L);
    }

    @Test
    void shouldRejectTransferWhenFundsAreInsufficient() {
        fundAlice(100_00L);

        PostLedgerTransactionCommand transfer = CommandFactory.twoLegCommand(
                UUID.randomUUID(),
                aliceId,
                bobId,
                150_00L,
                LedgerTransactionType.TRANSFER
        );

        assertThatThrownBy(
                () -> postingService.post(transfer)
        ).isInstanceOf(InsufficientFundsException.class);

        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(entryRepository.count()).isEqualTo(2);
        assertThat(balance(aliceId)).isEqualTo(100_00L);
        assertThat(balance(bobId)).isZero();
    }

    @Test
    void shouldPreventConcurrentDoubleSpending()
            throws InterruptedException, ExecutionException, TimeoutException {
        fundAlice(100_00L);

        PostLedgerTransactionCommand commandFromAliceToBob =
                CommandFactory.twoLegCommand(
                        UUID.randomUUID(),
                        aliceId,
                        bobId,
                        80_00L,
                        LedgerTransactionType.TRANSFER
                );
        PostLedgerTransactionCommand commandFromAliceToCarol =
                CommandFactory.twoLegCommand(
                        UUID.randomUUID(),
                        aliceId,
                        carolId,
                        80_00L,
                        LedgerTransactionType.TRANSFER
                );
        var results = runConcurrently(
                List.of(
                        () -> postingService.post(commandFromAliceToBob),
                        () -> postingService.post(commandFromAliceToCarol)
                )
        );

        var successes = results.stream().filter(ConcurrentResult::succeeded).toList();

        List<Throwable> errors = results.stream()
                .filter(ConcurrentResult::failed)
                .map(ConcurrentResult::error)
                .toList();

        assertThat(successes).hasSize(1);
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst()).isInstanceOf(InsufficientFundsException.class);

        assertThat(balance(aliceId)).isEqualTo(20_00L);

        long recipientsTotal = Math.addExact(
                balance(bobId),
                balance(carolId)
        );
        long customerFunds = Math.addExact(
                balance(aliceId),
                Math.addExact(
                        balance(bobId),
                        balance(carolId)
                )
        );

        assertThat(recipientsTotal).isEqualTo(80_00L);
        assertThat(customerFunds).isEqualTo(100_00L);
        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(entryRepository.count()).isEqualTo(4);
    }

    @Test
    void shouldHandleConcurrentIdempotentReplay()
            throws InterruptedException, ExecutionException, TimeoutException {

        UUID operationId =
                UUID.randomUUID();

        PostLedgerTransactionCommand command =
                CommandFactory.twoLegCommand(
                        operationId,
                        settlementId,
                        aliceId,
                        100_00L,
                        LedgerTransactionType.FUNDING
                );

        List<ConcurrentResult<PostLedgerTransactionResult>> results =
                runConcurrently(
                        List.of(
                                () -> postingService.post(command),
                                () -> postingService.post(command)
                        )
                );

        assertThat(results)
                .allMatch(ConcurrentResult::succeeded);

        List<PostLedgerTransactionResult> values =
                results.stream()
                        .map(ConcurrentResult::value)
                        .toList();

        assertThat(
                values.get(0).transactionId()
        ).isEqualTo(
                values.get(1).transactionId()
        );

        assertThat(
                values.stream()
                        .map(PostLedgerTransactionResult::replayed)
                        .toList()
        ).containsExactlyInAnyOrder(
                false,
                true
        );

        assertThat(transactionRepository.count())
                .isEqualTo(1);

        assertThat(entryRepository.count())
                .isEqualTo(2);

        assertThat(balance(aliceId))
                .isEqualTo(100_00L);
    }
}
