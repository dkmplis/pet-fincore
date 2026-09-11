package by.dkmplis.transfer_service.infrastructure.outbox.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.service.TransferService;
import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.transfer_service.infrastructure.outbox.publisher.ClaimedOutboxEvent;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class OutboxClaimServiceTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxClaimService claimService;


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
