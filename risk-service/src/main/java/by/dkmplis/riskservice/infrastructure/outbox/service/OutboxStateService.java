package by.dkmplis.riskservice.infrastructure.outbox.service;

import by.dkmplis.riskservice.infrastructure.outbox.exception.OutboxClaimLostException;
import by.dkmplis.riskservice.infrastructure.outbox.persistence.OutboxEventRepository;
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
            UUID outboxId,
            UUID claimToken
    ) {
        int updated =
                outboxEventRepository.markPublished(
                        outboxId,
                        claimToken,
                        Instant.now()
                );

        if (updated != 1) {
            throw new OutboxClaimLostException(
                    outboxId
            );
        }
    }

    @Transactional
    public void markFailed(
            UUID outboxId,
            UUID claimToken,
            String error
    ) {
        int updated =
                outboxEventRepository.markFailed(
                        outboxId,
                        claimToken,
                        error
                );

        if (updated != 1) {
            throw new OutboxClaimLostException(
                    outboxId
            );
        }
    }
}
