package by.dkmplis.ledgerservice.application.service;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.Posting;
import by.dkmplis.ledgerservice.application.exception.IdempotencyConflictException;
import by.dkmplis.ledgerservice.application.exception.InsufficientFundsException;
import by.dkmplis.ledgerservice.domain.enums.*;
import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerAccountRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerEntryRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
public class LedgerPostingServiceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRE_SQL_CONTAINER =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("ledger_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private LedgerPostingService postingService;
    @Autowired
    private LedgerBalanceService balanceService;
    @Autowired
    private LedgerAccountRepository accountRepository;
    @Autowired
    private LedgerTransactionRepository transactionRepository;
    @Autowired
    private LedgerEntryRepository entryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID settlementId;
    private UUID aliceId;
    private UUID bobId;
    private UUID carolId;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("""
        TRUNCATE TABLE
            ledger_entries,
            ledger_transactions,
            ledger_accounts
        CASCADE
        """);
        settlementId = UUID.randomUUID();
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();
        carolId = UUID.randomUUID();


        LedgerAccount settlement =
                new LedgerAccount(
                        settlementId,
                        AccountClass.ASSET,
                        AccountPurpose.SETTLEMENT,
                        "BYN",
                        AccountStatus.ACTIVE,
                        false
                );
        LedgerAccount alice =
                new LedgerAccount(
                        aliceId,
                        AccountClass.LIABILITY,
                        AccountPurpose.CUSTOMER_FUNDS,
                        "BYN",
                        AccountStatus.ACTIVE,
                        false
                );
        LedgerAccount bob =
                new LedgerAccount(
                        bobId,
                        AccountClass.LIABILITY,
                        AccountPurpose.CUSTOMER_FUNDS,
                        "BYN",
                        AccountStatus.ACTIVE,
                        false
                );
        LedgerAccount carol =
                new LedgerAccount(
                        carolId,
                        AccountClass.LIABILITY,
                        AccountPurpose.CUSTOMER_FUNDS,
                        "BYN",
                        AccountStatus.ACTIVE,
                        false
                );

        accountRepository.saveAllAndFlush(
                List.of(
                        settlement,
                        alice,
                        bob,
                        carol
                )
        );
    }

    @Test
    void shouldPostFundingAndTransfer() {
        fundAlice(100_00L);

        assertThat(balance(aliceId)).isEqualTo(100_00L);

        UUID operationId = UUID.randomUUID();

        PostLedgerTransactionCommand transfer = createCommand(
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
                createCommand(
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
                createCommand(
                        operationId,
                        settlementId,
                        aliceId,
                        100_00L,
                        LedgerTransactionType.FUNDING)
        );

        PostLedgerTransactionCommand conflicting =
                createCommand(
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

        PostLedgerTransactionCommand transfer = createCommand(
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
                createCommand(
                        UUID.randomUUID(),
                        aliceId,
                        bobId,
                        80_00L,
                        LedgerTransactionType.TRANSFER
                );
        PostLedgerTransactionCommand commandFromAliceToCarol =
                createCommand(
                        UUID.randomUUID(),
                        aliceId,
                        carolId,
                        80_00L,
                        LedgerTransactionType.TRANSFER
                );
        try (
                ExecutorService executor = Executors.newFixedThreadPool(2)
        ) {

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Throwable> first = () -> {
                ready.countDown();
                start.await();
                try {
                    postingService.post(commandFromAliceToBob);
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            };
            Callable<Throwable> second = () -> {
                ready.countDown();
                start.await();
                try {
                    postingService.post(commandFromAliceToCarol);
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            };

            Future<Throwable> firstResult = executor.submit(first);
            Future<Throwable> secondResult = executor.submit(second);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            Throwable firstError =
                    firstResult.get(
                            10,
                            TimeUnit.SECONDS
                    );

            Throwable secondError =
                    secondResult.get(
                            10,
                            TimeUnit.SECONDS
                    );

            executor.shutdownNow();

            List<Throwable> errors = Stream.of(firstError, secondError)
                    .filter(Objects::nonNull)
                    .toList();

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
    }

    @Test
    void shouldHandleConcurrentIdempotentReplay()
            throws InterruptedException, ExecutionException, TimeoutException {
        UUID operationId = UUID.randomUUID();

        PostLedgerTransactionCommand command = createCommand(
                operationId,
                settlementId,
                aliceId,
                100_00L,
                LedgerTransactionType.FUNDING
        );

        try (ExecutorService executorService =
                Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<PostLedgerTransactionResult> task = () -> {
                ready.countDown();
                start.countDown();
                return postingService.post(command);
            };

            Future<PostLedgerTransactionResult> first = executorService.submit(task);
            Future<PostLedgerTransactionResult> second = executorService.submit(task);

            assertThat(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            start.countDown();

            PostLedgerTransactionResult firstResult =
                    first.get(
                            10,
                            TimeUnit.SECONDS
                    );

            PostLedgerTransactionResult secondResult =
                    second.get(
                            10,
                            TimeUnit.SECONDS
                    );

            assertThat(firstResult.transactionId())
                    .isEqualTo(
                            secondResult.transactionId()
                    );

            assertThat(
                    List.of(
                            firstResult.replayed(),
                            secondResult.replayed()
                    )
            )
                    .containsExactlyInAnyOrder(
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

    private void fundAlice(long amountMinor) {
        postingService.post(
            createCommand(
                    UUID.randomUUID(),
                    settlementId,
                    aliceId,
                    amountMinor,
                    LedgerTransactionType.FUNDING
            )
        );
    }

    private PostLedgerTransactionCommand createCommand(
            UUID operationId,
            UUID fromAccountId,
            UUID toAccountId,
            long amountMinor,
            LedgerTransactionType type
    ) {
        return new PostLedgerTransactionCommand(
                operationId,
                type,
                "BYN",
                null,
                List.of(
                        new Posting(
                                fromAccountId,
                                PostingSide.DEBIT,
                                amountMinor
                        ),
                        new Posting(
                                toAccountId,
                                PostingSide.CREDIT,
                                amountMinor
                        )
                )
        );
    }

    private long balance(UUID accountId) {
        LedgerAccount account = accountRepository.findById(accountId)
                .orElseThrow();
        return balanceService.getBalanceMinor(account);
    }

}
