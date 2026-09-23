package by.dkmplis.riskservice.infrastructure.outbox.service;

import by.dkmplis.riskservice.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.riskservice.infrastructure.outbox.persistence.OutboxEventRepository;
import by.dkmplis.riskservice.infrastructure.outbox.publisher.ClaimedOutboxEvent;

import by.dkmplis.riskservice.support.AbstractRiskIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxClaimServiceTest extends AbstractRiskIntegrationTest {

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxStateService stateService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldClaimUnpublishedEventsInCreatedAtOrder() {
        Instant now = Instant.now();

        OutboxEvent first = saveEvent(
                now.minusSeconds(2)
        );

        OutboxEvent second = saveEvent(
                now.minusSeconds(1)
        );

        List<ClaimedOutboxEvent> claimed =
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(1)
                );

        assertThat(claimed)
                .extracting(ClaimedOutboxEvent::id)
                .containsExactly(
                        first.getId(),
                        second.getId()
                );

        assertThat(claimed.getFirst().claimToken())
                .isEqualTo(
                        claimed.getLast().claimToken()
                );

        assertThat(claimed.getFirst().key())
                .isEqualTo(first.getEventKey());

        OutboxEvent claimedFirst =
                outboxEventRepository
                        .findById(first.getId())
                        .orElseThrow();

        OutboxEvent claimedSecond =
                outboxEventRepository
                        .findById(second.getId())
                        .orElseThrow();

        assertThat(claimedFirst.getClaimToken())
                .isNotNull();
        assertThat(claimedFirst.getClaimedAt())
                .isNotNull();
        assertThat(claimedFirst.getAttempts())
                .isEqualTo(1);

        assertThat(claimedSecond.getClaimToken())
                .isNotNull();
        assertThat(claimedSecond.getClaimedAt())
                .isNotNull();
        assertThat(claimedSecond.getAttempts())
                .isEqualTo(1);
    }

    @Test
    void shouldSkipActiveClaimAndPublishedEvent() {
        Instant now = Instant.now();

        OutboxEvent activeClaim =
                saveEvent(now.minusSeconds(3));

        OutboxEvent published =
                saveEvent(now.minusSeconds(2));

        OutboxEvent free =
                saveEvent(now.minusSeconds(1));

        ClaimedOutboxEvent firstClaim =
                claimService.claimBatch(
                        1,
                        Duration.ofMinutes(1)
                ).getFirst();

        assertThat(firstClaim.id())
                .isEqualTo(activeClaim.getId());

        ClaimedOutboxEvent secondClaim =
                claimService.claimBatch(
                        1,
                        Duration.ofMinutes(1)
                ).getFirst();

        assertThat(secondClaim.id())
                .isEqualTo(published.getId());

        stateService.markPublished(
                secondClaim.id(),
                secondClaim.claimToken()
        );

        List<ClaimedOutboxEvent> remaining =
                claimService.claimBatch(
                        10,
                        Duration.ofMinutes(1)
                );

        assertThat(remaining)
                .extracting(ClaimedOutboxEvent::id)
                .containsExactly(free.getId());
    }

    @Test
    void shouldReclaimExpiredEventWithNewTokenAndIncrementAttempts() {
        OutboxEvent event =
                saveEvent(Instant.now());

        ClaimedOutboxEvent firstClaim =
                claimService.claimBatch(
                        1,
                        Duration.ofMinutes(1)
                ).getFirst();

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET claimed_at = ?
                WHERE id = ?
                """,
                Timestamp.from(
                        Instant.now().minusSeconds(120)
                ),
                event.getId()
        );

        ClaimedOutboxEvent reclaimed =
                claimService.claimBatch(
                        1,
                        Duration.ofMinutes(1)
                ).getFirst();

        assertThat(reclaimed.id())
                .isEqualTo(event.getId());

        assertThat(reclaimed.claimToken())
                .isNotEqualTo(
                        firstClaim.claimToken()
                );

        OutboxEvent stored =
                outboxEventRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(stored.getClaimToken())
                .isEqualTo(
                        reclaimed.claimToken()
                );

        assertThat(stored.getAttempts())
                .isEqualTo(2);
    }

    @Test
    void shouldRespectBatchSize() {
        Instant now = Instant.now();

        saveEvent(now.minusSeconds(3));
        saveEvent(now.minusSeconds(2));
        saveEvent(now.minusSeconds(1));

        List<ClaimedOutboxEvent> claimed =
                claimService.claimBatch(
                        2,
                        Duration.ofMinutes(1)
                );

        assertThat(claimed)
                .hasSize(2);
    }

    private OutboxEvent saveEvent(
            Instant createdAt
    ) {
        UUID aggregateId =
                UUID.randomUUID();

        OutboxEvent event =
                new OutboxEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "risk.events.v1",
                        aggregateId.toString(),
                        "risk.approved",
                        1,
                        aggregateId,
                        """
                        {
                          "eventType": "risk.approved"
                        }
                        """,
                        createdAt
                );

        return outboxEventRepository
                .saveAndFlush(event);
    }
}