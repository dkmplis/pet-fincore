package by.dkmplis.transfer_service.infrastructure.outbox.service;

import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEvent;
import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEventRepository;
import by.dkmplis.transfer_service.infrastructure.outbox.publisher.ClaimedOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<ClaimedOutboxEvent> claimBatch(
            int limit,
            Duration claimTimeout
    ) {
        Instant now = Instant.now();

        Instant claimBefore = now.minus(claimTimeout);

        List<OutboxEvent> events =
                outboxEventRepository.findClaimableForUpdate(
                        claimBefore,
                        limit
                );

        UUID claimToken = UUID.randomUUID();

        return events.stream()
                .map(event -> {
                    event.claim(
                            claimToken,
                            now
                    );

                    return new ClaimedOutboxEvent(
                            event.getId(),
                            claimToken,
                            event.getTopic(),
                            event.getAggregateId().toString(),
                            event.getPayload()
                    );
                })
                .toList();
    }
}
