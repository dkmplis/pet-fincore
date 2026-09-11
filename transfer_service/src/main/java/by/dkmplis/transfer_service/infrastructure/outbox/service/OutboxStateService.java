package by.dkmplis.transfer_service.infrastructure.outbox.service;

import by.dkmplis.transfer_service.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.transfer_service.infrastructure.outbox.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxStateService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markPublished(
            UUID eventId,
            UUID claimToken
    ) {
        int updated =
                outboxEventRepository.markPublished(
                        eventId,
                        claimToken,
                        Instant.now()
                );

        if (updated != 1) {
            throw new OutboxClaimLostException(
                    eventId
            );
        }
    }

    @Transactional
    public void markFailed(
            UUID eventId,
            UUID claimToken,
            String error
    ) {
        int updated =
                outboxEventRepository.markFailed(
                        eventId,
                        claimToken,
                        error
                );

        if (updated != 1) {
            throw new OutboxClaimLostException(
                    eventId
            );
        }
    }
}
