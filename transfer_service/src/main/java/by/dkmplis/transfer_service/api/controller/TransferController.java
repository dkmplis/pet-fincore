package by.dkmplis.transfer_service.api.controller;

import by.dkmplis.transfer_service.api.dto.CreateTransferRequest;
import by.dkmplis.transfer_service.application.command.TransferDetailsResponse;
import by.dkmplis.transfer_service.api.dto.TransferDetailsResult;
import by.dkmplis.transfer_service.api.dto.TransferResponse;
import by.dkmplis.transfer_service.api.mapper.TransferApiMapper;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.service.CreateTransferUseCase;
import by.dkmplis.transfer_service.application.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final CreateTransferUseCase createTransferUseCase;
    private final TransferApiMapper transferApiMapper;
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @RequestHeader("Idempotency-Key")
            UUID externalOperationId,
            @Valid @RequestBody
            CreateTransferRequest request
    ) {
        CreateTransferResult result =
                createTransferUseCase.execute(
                        transferApiMapper.toCommand(
                                request,
                                externalOperationId
                        )
                );

        TransferResponse response =
                transferApiMapper.toResponse(result);

        if (result.replayed()) {
            return ResponseEntity.ok(response);
        }

        URI location = URI.create(
                "/api/v1/transfers/" + result.transferId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferDetailsResponse> get(
            @PathVariable UUID transferId
    ) {
        TransferDetailsResult result =
                transferService.get(transferId);

        return ResponseEntity.ok(
                transferApiMapper.toResponse(result)
        );
    }


}
