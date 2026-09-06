package by.dkmplis.transfer_service.infrastructure.outbox;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.service.TransferService;
import by.dkmplis.transfer_service.application.service.TransferStateService;
import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TransactionalOutboxTest
    extends AbstractTransferIntegrationTest {
    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferStateService transferStateService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldPersistTransferCreatedEvent() {
        CreateTransferResult result =
                transferService.create(command());

        List<OutboxEvent> events =
                eventsFor(result.transferId());

        assertThat(events)
                .hasSize(1);

        OutboxEvent event = events.getFirst();

        assertThat(event.getAggregateType())
                .isEqualTo("TRANSFER");

        assertThat(event.getAggregateId())
                .isEqualTo(result.transferId());

        assertThat(event.getEventType())
                .isEqualTo("transfer.created");

        assertThat(event.getEventVersion())
                .isEqualTo(1);

        assertThat(event.getTopic())
                .isEqualTo("transfers.events.v1");

        assertThat(event.getPublishedAt())
                .isNull();

        assertThat(event.getAttempts())
                .isZero();
    }

    @Test
    void shouldNotCreateDuplicateEventForIdempotentReplay() {
        CreateTransferCommand command =
                command();

        CreateTransferResult first =
                transferService.create(command);

        CreateTransferResult replay =
                transferService.create(command);

        assertThat(replay.transferId())
                .isEqualTo(first.transferId());

        assertThat(replay.replayed())
                .isTrue();

        assertThat(
                eventsFor(first.transferId())
        )
                .extracting(
                        OutboxEvent::getEventType
                )
                .containsExactly(
                        "transfer.created"
                );
    }

    @Test
    void shouldPersistCompletedEvent() {
        CreateTransferResult created =
                transferService.create(command());

        UUID ledgerTransactionId =
                UUID.randomUUID();

        transferStateService.markCompleted(
                created.transferId(),
                ledgerTransactionId
        );

        assertThat(
                eventsFor(created.transferId())
        )
                .extracting(
                        OutboxEvent::getEventType
                )
                .containsExactlyInAnyOrder(
                        "transfer.created",
                        "transfer.completed"
                );
    }

    @Test
    void shouldPersistRejectedEvent() {
        CreateTransferResult created =
                transferService.create(command());

        transferStateService.markRejected(
                created.transferId()
        );

        assertThat(
                eventsFor(created.transferId())
        )
                .extracting(
                        OutboxEvent::getEventType
                )
                .containsExactlyInAnyOrder(
                        "transfer.created",
                        "transfer.rejected"
                );
    }

    private List<OutboxEvent> eventsFor(
            UUID transferId
    ) {
        return outboxEventRepository
                .findAll()
                .stream()
                .filter(event ->
                        event.getAggregateId()
                                .equals(transferId)
                )
                .toList();
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
