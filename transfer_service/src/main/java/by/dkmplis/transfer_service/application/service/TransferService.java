package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.api.dto.TransferDetailsResult;
import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.exception.TransferIdempotencyConflictException;
import by.dkmplis.transfer_service.application.exception.TransferNotFoundException;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.infrastructure.persistence.TransferOperationLockRepository;
import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferRequestFingerprintCalculator fingerprintCalculator;
    private final TransferOperationLockRepository operationLockRepository;

    @Transactional
    public CreateTransferResult create(CreateTransferCommand command) {
        String fingerprint = fingerprintCalculator.calculate(command);

        operationLockRepository.lock(command.externalOperationId());

        Optional<Transfer> existing = transferRepository.findByExternalOperationId(
                command.externalOperationId()
        );

        if (existing.isPresent()) {
            return handleExisting(
                    existing.get(),
                    fingerprint
            );
        }

        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                command.externalOperationId(),
                fingerprint,
                UUID.randomUUID(),
                command.fromAccountId(),
                command.toAccountId(),
                command.currency(),
                command.amountMinor()
        );

        Transfer savedTransfer = transferRepository.save(transfer);

        return new CreateTransferResult(
                savedTransfer.getId(),
                transfer.getState(),
                false
        );
    }

    @Transactional(readOnly = true)
    public TransferDetailsResult get(UUID transferId) {
        Transfer transfer = transferRepository
                .findById(transferId)
                .orElseThrow(
                        () -> new TransferNotFoundException(
                                transferId
                        )
                );

        return new TransferDetailsResult(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getCurrency(),
                transfer.getAmountMinor(),
                transfer.getState()
        );
    }

    private CreateTransferResult handleExisting(
            Transfer existing,
            String fingerprint
    ) {
        if (!existing.getRequestFingerprint()
                .equals(fingerprint)) {

            throw new TransferIdempotencyConflictException(
                    existing.getExternalOperationId()
            );
        }

        return new CreateTransferResult(
                existing.getId(),
                existing.getState(),
                true
        );
    }
}
