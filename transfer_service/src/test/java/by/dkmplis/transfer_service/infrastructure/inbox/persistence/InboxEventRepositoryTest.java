package by.dkmplis.transfer_service.infrastructure.inbox.persistence;

import by.dkmplis.transfer_service.support.AbstractTransferIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InboxEventRepositoryTest
        extends AbstractTransferIntegrationTest {

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Test
    @Transactional
    void shouldRegisterEventOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();

        int first = inboxEventRepository.register(
                eventId,
                "risk.approved",
                1,
                transferId,
                """
                {
                  "eventType": "risk.approved"
                }
                """
        );

        int duplicate = inboxEventRepository.register(
                eventId,
                "risk.approved",
                1,
                transferId,
                """
                {
                  "eventType": "risk.approved"
                }
                """
        );

        assertThat(first)
                .isEqualTo(1);

        assertThat(duplicate)
                .isZero();

        InboxEvent persisted =
                inboxEventRepository
                        .findById(eventId)
                        .orElseThrow();

        assertThat(persisted.getAggregateId())
                .isEqualTo(transferId);

        assertThat(persisted.getProcessedAt())
                .isNull();
    }

    @Test
    @Transactional
    void shouldMarkInboxEventAsProcessedOnlyOnce() {
        UUID eventId = UUID.randomUUID();

        inboxEventRepository.register(
                eventId,
                "risk.rejected",
                1,
                UUID.randomUUID(),
                "{}"
        );

        int first =
                inboxEventRepository.markProcessed(
                        eventId,
                        Instant.now()
                );

        int second =
                inboxEventRepository.markProcessed(
                        eventId,
                        Instant.now()
                );

        assertThat(first)
                .isEqualTo(1);

        assertThat(second)
                .isZero();

        InboxEvent persisted =
                inboxEventRepository
                        .findById(eventId)
                        .orElseThrow();

        assertThat(persisted.getProcessedAt())
                .isNotNull();
    }
}