package by.dkmplis.transfer_service.infrastructure.outbox.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.service.TransferService;
import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.transfer_service.infrastructure.outbox.publisher.ClaimedOutboxEvent;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OutboxClaimServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @Test
    void shouldClaimUnpublishedEvent() {
        CreateTransferResult transfer =
                transferService.create(command());

        List<ClaimedOutboxEvent> claimed =
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                );

        assertThat(claimed)
                .hasSize(1);

        ClaimedOutboxEvent event =
                claimed.getFirst();

        assertThat(event.id())
                .isNotNull();

        assertThat(event.claimToken())
                .isNotNull();

        assertThat(event.key())
                .isEqualTo(
                        transfer.transferId()
                                .toString()
                );

        assertThat(event.topic())
                .isEqualTo(
                        "transfers.events.v1"
                );

        assertThat(event.payload())
                .isNotBlank();
    }

    @Test
    void shouldNotClaimAlreadyClaimedEvent() {
        transferService.create(
                command()
        );

        List<ClaimedOutboxEvent> first =
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                );

        List<ClaimedOutboxEvent> second =
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                );

        assertThat(first)
                .hasSize(1);

        assertThat(second)
                .isEmpty();
    }

    @Test
    void shouldReclaimStaleClaim() {
        CreateTransferResult transfer =
                transferService.create(command());

        OutboxEvent persisted =
                outboxEventRepository
                        .findAllByAggregateIdOrderByCreatedAtAsc(
                                transfer.transferId()
                        )
                        .getFirst();

        UUID oldClaimToken =
                UUID.randomUUID();

        persisted.claim(
                oldClaimToken,
                Instant.now()
                        .minus(Duration.ofMinutes(5))
        );

        outboxEventRepository.saveAndFlush(
                persisted
        );

        List<ClaimedOutboxEvent> reclaimed =
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                );

        assertThat(reclaimed)
                .hasSize(1);

        ClaimedOutboxEvent event =
                reclaimed.getFirst();

        assertThat(event.id())
                .isEqualTo(persisted.getId());

        assertThat(event.claimToken())
                .isNotEqualTo(oldClaimToken);

        OutboxEvent afterReclaim =
                outboxEventRepository
                        .findById(event.id())
                        .orElseThrow();

        assertThat(afterReclaim.getClaimToken())
                .isEqualTo(event.claimToken());

        assertThat(afterReclaim.getAttempts())
                .isEqualTo(2);
    }

    @Test
    void shouldSkipRowLockedByAnotherWorker() throws Exception {
        Instant createdAt =
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                );

        UUID firstId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        UUID secondId =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                );

        outboxEventRepository.saveAllAndFlush(
                List.of(
                        event(
                                firstId,
                                createdAt
                        ),
                        event(
                                secondId,
                                createdAt.plusSeconds(1)
                        )
                )
        );

        CountDownLatch firstRowLocked =
                new CountDownLatch(1);

        CountDownLatch releaseFirstTransaction =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        Future<UUID> firstWorker =
                executor.submit(
                        () -> transactionTemplate.execute(
                                status -> {
                                    List<OutboxEvent> locked =
                                            outboxEventRepository
                                                    .findClaimableForUpdate(
                                                            Instant.now()
                                                                    .minus(
                                                                            Duration.ofMinutes(1)
                                                                    ),
                                                            1
                                                    );

                                    UUID lockedId =
                                            locked.getFirst()
                                                    .getId();

                                    firstRowLocked.countDown();

                                    try {
                                        boolean released =
                                                releaseFirstTransaction.await(
                                                        5,
                                                        TimeUnit.SECONDS
                                                );

                                        if (!released) {
                                            throw new IllegalStateException(
                                                    "Timed out waiting to release transaction"
                                            );
                                        }

                                    } catch (InterruptedException exception) {
                                        Thread.currentThread()
                                                .interrupt();

                                        throw new IllegalStateException(
                                                exception
                                        );
                                    }

                                    return lockedId;
                                }
                        )
                );

        try {
            assertThat(
                    firstRowLocked.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            Future<List<ClaimedOutboxEvent>> secondWorker =
                    executor.submit(
                            () -> claimService.claimBatch(
                                    1,
                                    Duration.ofMinutes(1)
                            )
                    );

            List<ClaimedOutboxEvent> claimed =
                    secondWorker.get(
                            2,
                            TimeUnit.SECONDS
                    );

            assertThat(claimed)
                    .hasSize(1);

            assertThat(claimed.getFirst().id())
                    .isEqualTo(secondId);

        } finally {
            releaseFirstTransaction.countDown();

            assertThat(
                    firstWorker.get(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isEqualTo(firstId);

            executor.shutdownNow();
        }
    }

    private OutboxEvent event(
            UUID id,
            Instant createdAt
    ) {
        return new OutboxEvent(
                id,
                "TRANSFER",
                UUID.randomUUID(),
                "transfer.created",
                1,
                "transfers.events.v1",
                "{}",
                createdAt
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
