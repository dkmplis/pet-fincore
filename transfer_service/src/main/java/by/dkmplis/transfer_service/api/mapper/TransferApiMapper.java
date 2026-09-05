package by.dkmplis.transfer_service.api.mapper;

import by.dkmplis.transfer_service.api.dto.CreateTransferRequest;
import by.dkmplis.transfer_service.application.command.TransferDetailsResponse;
import by.dkmplis.transfer_service.api.dto.TransferDetailsResult;
import by.dkmplis.transfer_service.api.dto.TransferResponse;
import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransferApiMapper {

    CreateTransferCommand toCommand(
            CreateTransferRequest request,
            UUID externalOperationId
    );

    TransferResponse toResponse(
            CreateTransferResult result
    );

    TransferDetailsResponse toResponse(
            TransferDetailsResult result
    );
}
