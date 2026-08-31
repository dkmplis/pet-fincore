package by.dkmplis.ledgerservice.api.controller;

import by.dkmplis.ledgerservice.api.dto.LedgerTransactionResponse;
import by.dkmplis.ledgerservice.api.dto.PostLedgerTransactionRequest;
import by.dkmplis.ledgerservice.api.mapper.LedgerTransactionApiMapper;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.ReverseLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.service.LedgerPostingService;
import by.dkmplis.ledgerservice.application.service.LedgerReversalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController()
@RequestMapping("api/v1/ledger/transactions")
@RequiredArgsConstructor
public class LedgerTransactionController {

    private final LedgerPostingService postingService;
    private final LedgerReversalService reversalService;
    private final LedgerTransactionApiMapper transactionApiMapper;

    @PostMapping()
    public ResponseEntity<LedgerTransactionResponse> post(
            @RequestHeader("Idempotency-Key")
            UUID externalOperationId,
            @Valid @RequestBody
            PostLedgerTransactionRequest transactionRequest
    ) {
        PostLedgerTransactionResult result = postingService.post(
                transactionApiMapper.toCommand(
                        transactionRequest, externalOperationId
                )
        );

        return ResponseEntity.ok(transactionApiMapper.toResponse(result));
    }

    @PostMapping("{transactionId}/reversal")
    public ResponseEntity<LedgerTransactionResponse> postReversal(
            @RequestHeader("Idempotency-Key")
            UUID externalOperationId,
            @PathVariable
            UUID transactionId
    ) {
        PostLedgerTransactionResult result = reversalService.reverse(
                new ReverseLedgerTransactionCommand(
                        externalOperationId,
                        transactionId
                )
        );

        return ResponseEntity.ok(transactionApiMapper.toResponse(result));

    }
}
