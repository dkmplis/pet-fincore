package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.exception.LedgerCallUncertainException;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTransferUseCase {

    private final TransferService transferService;
    private final TransferProcessingService processingService;

    public CreateTransferResult execute(
            CreateTransferCommand command
    ) {
        CreateTransferResult created =
                transferService.create(command);

        if (created.state() != TransferState.PENDING) {
            return created;
        }

        try {
            TransferState state =
                    processingService.process(
                            created.transferId()
                    );

            return new CreateTransferResult(
                    created.transferId(),
                    state,
                    created.replayed()
            );

        } catch (LedgerCallUncertainException exception) {
            return new CreateTransferResult(
                    created.transferId(),
                    TransferState.PENDING,
                    created.replayed()
            );
        }
    }
}
