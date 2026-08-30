package by.dkmplis.ledgerservice.support;

import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.service.LedgerBalanceService;
import by.dkmplis.ledgerservice.application.service.LedgerPostingService;
import by.dkmplis.ledgerservice.domain.enums.AccountClass;
import by.dkmplis.ledgerservice.domain.enums.AccountPurpose;
import by.dkmplis.ledgerservice.domain.enums.AccountStatus;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.domain.model.LedgerAccount;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerAccountRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerEntryRepository;
import by.dkmplis.ledgerservice.infrastructure.persistence.repositories.LedgerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(LedgerTestcontainersConfiguration.class)
public abstract class AbstractLedgerIntegrationTest {
    @Autowired
    protected LedgerPostingService postingService;

    @Autowired
    protected LedgerBalanceService balanceService;

    @Autowired
    protected LedgerAccountRepository accountRepository;

    @Autowired
    protected LedgerTransactionRepository transactionRepository;

    @Autowired
    protected LedgerEntryRepository entryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    protected UUID settlementId;
    protected UUID aliceId;
    protected UUID bobId;
    protected UUID carolId;

    @BeforeEach
    void setUpLedger() {

        cleanDatabase();

        settlementId = UUID.randomUUID();
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();
        carolId = UUID.randomUUID();

        LedgerAccount settlement = createAccount(
                settlementId,
                AccountClass.ASSET,
                AccountPurpose.SETTLEMENT
        );

        LedgerAccount alice = createAccount(
                aliceId,
                AccountClass.LIABILITY,
                AccountPurpose.CUSTOMER_FUNDS
        );

        LedgerAccount bob = createAccount(
                bobId,
                AccountClass.LIABILITY,
                AccountPurpose.CUSTOMER_FUNDS
        );

        LedgerAccount carol = createAccount(
                carolId,
                AccountClass.LIABILITY,
                AccountPurpose.CUSTOMER_FUNDS
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

    protected PostLedgerTransactionResult fundAlice(
            long amountMinor
    ) {
        return postingService.post(
                CommandFactory.twoLegCommand(
                        UUID.randomUUID(),
                        settlementId,
                        aliceId,
                        amountMinor,
                        LedgerTransactionType.FUNDING
                )
        );
    }

    protected <T> List<ConcurrentResult<T>> runConcurrently(
            List<Callable<T>> tasks
    ) throws InterruptedException, ExecutionException, TimeoutException {

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(tasks.size())) {

            CountDownLatch ready =
                    new CountDownLatch(tasks.size());

            CountDownLatch start =
                    new CountDownLatch(1);

            List<Future<ConcurrentResult<T>>> futures =
                    new ArrayList<>();

            for (Callable<T> task : tasks) {

                Callable<ConcurrentResult<T>> wrappedTask = () -> {

                    ready.countDown();
                    start.await();

                    try {
                        return ConcurrentResult.success(
                                task.call()
                        );
                    } catch (Throwable error) {
                        return ConcurrentResult.failure(
                                error
                        );
                    }
                };

                futures.add(
                        executor.submit(wrappedTask)
                );
            }

            assertThat(
                    ready.await(5, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            List<ConcurrentResult<T>> results =
                    new ArrayList<>();

            for (Future<ConcurrentResult<T>> future : futures) {
                results.add(
                        future.get(
                                10,
                                TimeUnit.SECONDS
                        )
                );
            }

            return results;
        }
    }

    protected long balance(UUID accountId) {

        LedgerAccount account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow();

        return balanceService.getBalanceMinor(account);
    }

    private LedgerAccount createAccount(
            UUID id,
            AccountClass accountClass,
            AccountPurpose purpose
    ) {
        return new LedgerAccount(
                id,
                accountClass,
                purpose,
                "BYN",
                AccountStatus.ACTIVE,
                false
        );
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE
                ledger_entries,
                ledger_transactions,
                ledger_accounts
            CASCADE
            """);
    }
}
