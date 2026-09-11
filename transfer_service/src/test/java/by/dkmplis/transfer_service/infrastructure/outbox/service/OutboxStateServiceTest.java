package by.dkmplis.transfer_service.infrastructure.outbox.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.service.TransferService;
import by.dkmplis.transfer_service.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.transfer_service.infrastructure.outbox.publisher.ClaimedOutboxEvent;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OutboxStateServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxStateService stateService;

    @Test
    void shouldMarkClaimedEventAsPublished() {
        CreateTransferResult transfer =
                transferService.create(command());

        ClaimedOutboxEvent claimed =
                claimService.claimBatch(
                                100,
                                Duration.ofMinutes(1)
                        )
                        .getFirst();

        stateService.markPublished(
                claimed.id(),
                claimed.claimToken()
        );

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(claimed.id())
                        .orElseThrow();

        assertThat(persisted.getPublishedAt())
                .isNotNull();

        assertThat(persisted.getClaimedAt())
                .isNull();

        assertThat(persisted.getClaimToken())
                .isNull();

        assertThat(persisted.getLastError())
                .isNull();
    }

    @Test
    void shouldReleaseClaimAfterPublishingFailure() {
        transferService.create(
                command()
        );

        ClaimedOutboxEvent claimed =
                claimService.claimBatch(
                                100,
                                Duration.ofMinutes(1)
                        )
                        .getFirst();

        stateService.markFailed(
                claimed.id(),
                claimed.claimToken(),
                "Kafka unavailable"
        );

        OutboxEvent persisted =
                outboxEventRepository
                        .findById(claimed.id())
                        .orElseThrow();

        assertThat(persisted.getPublishedAt())
                .isNull();

        assertThat(persisted.getClaimedAt())
                .isNull();

        assertThat(persisted.getClaimToken())
                .isNull();

        assertThat(persisted.getLastError())
                .isEqualTo(
                        "Kafka unavailable"
                );

        List<ClaimedOutboxEvent> retry =
                claimService.claimBatch(
                        100,
                        Duration.ofMinutes(1)
                );

        assertThat(retry)
                .hasSize(1);

        assertThat(retry.getFirst().id())
                .isEqualTo(claimed.id());

        assertThat(retry.getFirst().claimToken())
                .isNotEqualTo(
                        claimed.claimToken()
                );
    }

    @Test
    void shouldRejectStateUpdateFromPreviousClaimOwner() {
        transferService.create(
                command()
        );

        ClaimedOutboxEvent first =
                claimService.claimBatch(
                                100,
                                Duration.ofMinutes(1)
                        )
                        .getFirst();

        stateService.markFailed(
                first.id(),
                first.claimToken(),
                "Temporary failure"
        );

        ClaimedOutboxEvent second =
                claimService.claimBatch(
                                100,
                                Duration.ofMinutes(1)
                        )
                        .getFirst();

        assertThat(second.claimToken())
                .isNotEqualTo(first.claimToken());

        assertThatThrownBy(
                () -> stateService.markPublished(
                        first.id(),
                        first.claimToken()
                )
        ).isInstanceOf(
                OutboxClaimLostException.class
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
