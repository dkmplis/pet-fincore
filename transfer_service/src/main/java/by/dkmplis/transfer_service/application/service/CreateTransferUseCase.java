package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTransferUseCase {

    private final TransferService transferService;

    public CreateTransferResult execute(
            CreateTransferCommand command
    ) {
        return transferService.create(command);
    }
}
