package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.ReverseLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionAlreadyReversedException;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.enums.TransactionState;
import by.dkmplis.ledgerservice.support.AbstractLedgerIntegrationTest;
import by.dkmplis.ledgerservice.support.ConcurrentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LedgerReversalServiceTest extends AbstractLedgerIntegrationTest {
    @Autowired

    private LedgerReversalService reversalService;

    @Test
    void shouldReversePostedTransaction() {
        PostLedgerTransactionResult funding =
                fundAlice(100_00L);

        assertThat(balance(aliceId))
                .isEqualTo(100_00L);

        ReverseLedgerTransactionCommand command =
                new ReverseLedgerTransactionCommand(
                        UUID.randomUUID(),
                        funding.transactionId()
                );

        PostLedgerTransactionResult reversal =
                reversalService.reverse(command);

        assertThat(reversal.replayed())
                .isFalse();

        assertThat(balance(aliceId))
                .isZero();

        assertThat(balance(settlementId))
                .isZero();

        var reversalTransaction =
                transactionRepository
                        .findById(reversal.transactionId())
                        .orElseThrow();

        assertThat(reversalTransaction.getTransactionType())
                .isEqualTo(LedgerTransactionType.REVERSAL);

        assertThat(reversalTransaction.getReversesTransactionId())
                .isEqualTo(funding.transactionId());

        assertThat(reversalTransaction.getState())
                .isEqualTo(TransactionState.POSTED);

        assertThat(transactionRepository.count())
                .isEqualTo(2);

        assertThat(entryRepository.count())
                .isEqualTo(4);
    }

    @Test
    void shouldReturnReplayForSameReversalRequest() {

        PostLedgerTransactionResult funding =
                fundAlice(100_00L);

        UUID reversalOperationId =
                UUID.randomUUID();

        ReverseLedgerTransactionCommand command =
                new ReverseLedgerTransactionCommand(
                        reversalOperationId,
                        funding.transactionId()
                );

        PostLedgerTransactionResult first =
                reversalService.reverse(command);

        PostLedgerTransactionResult second =
                reversalService.reverse(command);

        assertThat(first.replayed())
                .isFalse();

        assertThat(second.replayed())
                .isTrue();

        assertThat(second.transactionId())
                .isEqualTo(first.transactionId());

        assertThat(transactionRepository.count())
                .isEqualTo(2);

        assertThat(entryRepository.count())
                .isEqualTo(4);

        assertThat(balance(aliceId))
                .isZero();
    }

    @Test
    void shouldRejectSecondReversalWithDifferentOperationId() {

        PostLedgerTransactionResult funding =
                fundAlice(100_00L);

        reversalService.reverse(
                new ReverseLedgerTransactionCommand(
                        UUID.randomUUID(),
                        funding.transactionId()
                )
        );

        assertThatThrownBy(
                () -> reversalService.reverse(
                        new ReverseLedgerTransactionCommand(
                                UUID.randomUUID(),
                                funding.transactionId()
                        )
                )
        ).isInstanceOf(
                LedgerTransactionAlreadyReversedException.class
        );

        assertThat(transactionRepository.count())
                .isEqualTo(2);

        assertThat(entryRepository.count())
                .isEqualTo(4);

        assertThat(balance(aliceId))
                .isZero();
    }

    @Test
    void shouldAllowOnlyOneConcurrentReversal()
            throws InterruptedException, ExecutionException, TimeoutException {
        PostLedgerTransactionResult funding = fundAlice(100_00L);

        ReverseLedgerTransactionCommand firstCommand = new ReverseLedgerTransactionCommand(
                UUID.randomUUID(),
                funding.transactionId()
        );
        ReverseLedgerTransactionCommand secondCommand = new ReverseLedgerTransactionCommand(
                UUID.randomUUID(),
                funding.transactionId()
        );

        List<ConcurrentResult<PostLedgerTransactionResult>> results =
                runConcurrently(
                        List.of(
                                () -> reversalService.reverse(firstCommand),
                                () -> reversalService.reverse(secondCommand)
                        )
                );
        List<PostLedgerTransactionResult> successes =
                results.stream()
                        .filter(ConcurrentResult::succeeded)
                        .map(ConcurrentResult::value)
                        .toList();

        List<Throwable> errors =
                results.stream()
                        .filter(ConcurrentResult::failed)
                        .map(ConcurrentResult::error)
                        .toList();

        assertThat(successes).hasSize(1);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst())
                .isInstanceOf(LedgerTransactionAlreadyReversedException.class);

        assertThat(balance(aliceId)).isZero();
        assertThat(balance(settlementId)).isZero();

        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(entryRepository.count()).isEqualTo(4);

        var reversal =
                transactionRepository
                        .findByReversesTransactionId(funding.transactionId())
                        .orElseThrow();

        assertThat(reversal.getTransactionType())
                .isEqualTo(LedgerTransactionType.REVERSAL);

        assertThat(reversal.getState())
                .isEqualTo(TransactionState.POSTED);
    }
}