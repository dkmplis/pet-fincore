package by.dkmplis.riskservice.infrastructure.outbox.service;

import by.dkmplis.riskservice.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.riskservice.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.riskservice.infrastructure.outbox.persistence.OutboxEventRepository;
import by.dkmplis.riskservice.infrastructure.outbox.publisher.ClaimedOutboxEvent;
import by.dkmplis.riskservice.support.AbstractRiskIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxStateServiceTest extends AbstractRiskIntegrationTest {

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxStateService stateService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldMarkPublishedAndClearClaimAndPreviousError() {
        OutboxEvent event =
                saveEvent();

        ClaimedOutboxEvent firstClaim =
                claim();

        stateService.markFailed(
                firstClaim.id(),
                firstClaim.claimToken(),
                "temporary Kafka failure"
        );

        ClaimedOutboxEvent retryClaim =
                claim();

        stateService.markPublished(
                retryClaim.id(),
                retryClaim.claimToken()
        );

        OutboxEvent stored =
                outboxEventRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(stored.getPublishedAt())
                .isNotNull();

        assertThat(stored.getClaimedAt())
                .isNull();

        assertThat(stored.getClaimToken())
                .isNull();

        assertThat(stored.getLastError())
                .isNull();

        assertThat(stored.getAttempts())
                .isEqualTo(2);
    }

    @Test
    void shouldMarkFailedReleaseClaimAndAllowRetry() {
        OutboxEvent event =
                saveEvent();

        ClaimedOutboxEvent firstClaim =
                claim();

        stateService.markFailed(
                firstClaim.id(),
                firstClaim.claimToken(),
                "broker unavailable"
        );

        OutboxEvent failed =
                outboxEventRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(failed.getPublishedAt())
                .isNull();

        assertThat(failed.getClaimedAt())
                .isNull();

        assertThat(failed.getClaimToken())
                .isNull();

        assertThat(failed.getLastError())
                .isEqualTo("broker unavailable");

        ClaimedOutboxEvent retryClaim =
                claim();

        assertThat(retryClaim.id())
                .isEqualTo(event.getId());

        assertThat(retryClaim.claimToken())
                .isNotEqualTo(
                        firstClaim.claimToken()
                );

        OutboxEvent retried =
                outboxEventRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(retried.getAttempts())
                .isEqualTo(2);
    }

    @Test
    void staleTokenShouldNotMarkEventPublishedOrFailed() {
        OutboxEvent event =
                saveEvent();

        ClaimedOutboxEvent currentClaim =
                claim();

        UUID staleToken =
                UUID.randomUUID();

        assertThatThrownBy(
                () -> stateService.markPublished(
                        event.getId(),
                        staleToken
                )
        )
                .isInstanceOf(
                        OutboxClaimLostException.class
                );

        assertThatThrownBy(
                () -> stateService.markFailed(
                        event.getId(),
                        staleToken,
                        "old worker failure"
                )
        )
                .isInstanceOf(
                        OutboxClaimLostException.class
                );

        OutboxEvent stored =
                outboxEventRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(stored.getPublishedAt())
                .isNull();

        assertThat(stored.getClaimToken())
                .isEqualTo(
                        currentClaim.claimToken()
                );

        assertThat(stored.getClaimedAt())
                .isNotNull();

        assertThat(stored.getLastError())
                .isNull();
    }

    private ClaimedOutboxEvent claim() {
        return claimService
                .claimBatch(
                        1,
                        Duration.ofMinutes(1)
                )
                .getFirst();
    }

    private OutboxEvent saveEvent() {
        UUID aggregateId =
                UUID.randomUUID();

        return outboxEventRepository.saveAndFlush(
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
                        Instant.now()
                )
        );
    }
}