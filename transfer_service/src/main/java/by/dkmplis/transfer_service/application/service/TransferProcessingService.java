package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.exception.LedgerTransferRejectedException;
import by.dkmplis.transfer_service.application.exception.TransferNotFoundException;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.application.port.LedgerTransferCommand;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.domain.model.Transfer;
import by.dkmplis.transfer_service.infrastructure.persistence.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferProcessingService {

    private final TransferRepository transferRepository;
    private final LedgerClient ledgerClient;
    private final TransferStateService stateService;

    public TransferState process(UUID transferId) {
        Transfer transfer = transferRepository
                .findById(transferId)
                .orElseThrow(
                        () -> new TransferNotFoundException(transferId)
                );

        if (transfer.getState() != TransferState.PENDING) {
            return transfer.getState();
        }

        LedgerTransferCommand command = new LedgerTransferCommand(
                transfer.getLedgerOperationId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getCurrency(),
                transfer.getAmountMinor()
        );

        try {

            LedgerTransferResult result = ledgerClient.postTransfer(command);
            return stateService.markCompleted(
                    transfer.getId(),
                    result.ledgerTransactionId()
            );

        } catch (LedgerTransferRejectedException exception) {
            return stateService.markRejected(
                    transfer.getId()
            );
        }
    }
}
