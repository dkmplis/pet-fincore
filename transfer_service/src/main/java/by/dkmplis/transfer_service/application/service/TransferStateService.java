package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.event.TransferCompletedEvent;
import by.dkmplis.transfer_service.application.event.TransferCompletedPayload;
import by.dkmplis.transfer_service.application.event.TransferRejectedEvent;
import by.dkmplis.transfer_service.application.event.TransferRejectedPayload;
import by.dkmplis.transfer_service.application.exception.TransferNotFoundException;
import by.dkmplis.transfer_service.application.port.IntegrationEventPublisher;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferStateService {

    private final TransferRepository transferRepository;
    private final IntegrationEventPublisher eventPublisher;

    @Transactional
    public TransferState markCompleted(
            UUID transferId,
            UUID ledgerTransactionId
    ) {
        Transfer transfer = transferRepository
                .findByIdForUpdate(transferId)
                .orElseThrow(
                        () -> new TransferNotFoundException(transferId)
                );

        if (transfer.getState() == TransferState.COMPLETED) {
            return TransferState.COMPLETED;
        }
        if (transfer.getState() == TransferState.REJECTED) {
            throw new IllegalStateException(
                    "Only pending transfer can be completed"
            );
        }

        transfer.complete(ledgerTransactionId);
        eventPublisher.publish(
                new TransferCompletedEvent(
                        UUID.randomUUID(),
                        transfer.getId(),
                        Instant.now(),
                        new TransferCompletedPayload(
                                ledgerTransactionId
                        )
                )
        );
        return transfer.getState();
    }

    @Transactional
    public TransferState markRejected(UUID transferId) {
        Transfer transfer = transferRepository
                .findByIdForUpdate(transferId)
                .orElseThrow(
                        () -> new TransferNotFoundException(transferId)
                );

        if (transfer.getState() == TransferState.REJECTED) {
            return TransferState.REJECTED;
        }

        if (transfer.getState() == TransferState.COMPLETED) {
            throw new IllegalStateException(
                    "Only pending transfer can be rejected"
            );
        }

        transfer.reject();

        eventPublisher.publish(
                new TransferRejectedEvent(
                        UUID.randomUUID(),
                        transfer.getId(),
                        Instant.now(),
                        new TransferRejectedPayload()
                )
        );

        return transfer.getState();
    }
}
