package by.dkmplis.ledgerservice.api.mapper;

import by.dkmplis.ledgerservice.api.dto.LedgerTransactionResponse;
import by.dkmplis.ledgerservice.api.dto.PostLedgerTransactionRequest;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.Posting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LedgerTransactionApiMapper {
    LedgerTransactionResponse toResponse(PostLedgerTransactionResult result);

    @Mapping(target = "reversesTransactionId", ignore = true)
    PostLedgerTransactionCommand toCommand(
            PostLedgerTransactionRequest request,
            UUID externalOperationId
    );

    Posting toPosting(PostLedgerTransactionRequest.PostingRequest request);
}
