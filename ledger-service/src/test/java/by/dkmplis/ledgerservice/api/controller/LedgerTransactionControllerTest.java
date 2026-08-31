package by.dkmplis.ledgerservice.api.controller;

import by.dkmplis.ledgerservice.api.dto.LedgerTransactionResponse;
import by.dkmplis.ledgerservice.api.dto.PostLedgerTransactionRequest;
import by.dkmplis.ledgerservice.api.mapper.LedgerTransactionApiMapper;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.command.PostLedgerTransactionResult;
import by.dkmplis.ledgerservice.application.command.ReverseLedgerTransactionCommand;
import by.dkmplis.ledgerservice.application.exception.IdempotencyConflictException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionNotFoundException;
import by.dkmplis.ledgerservice.application.service.LedgerPostingService;
import by.dkmplis.ledgerservice.application.service.LedgerReversalService;
import by.dkmplis.ledgerservice.domain.enums.LedgerTransactionType;
import by.dkmplis.ledgerservice.support.CommandFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerTransactionController.class)
class LedgerTransactionControllerTest {

    private static final String TRANSACTIONS_URL =
            "/api/v1/ledger/transactions";

    private static final String IDEMPOTENCY_KEY =
            "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerPostingService postingService;

    @MockitoBean
    private LedgerReversalService reversalService;

    @MockitoBean
    private LedgerTransactionApiMapper mapper;

    @Test
    void shouldPostLedgerTransaction() throws Exception {

        UUID operationId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        PostLedgerTransactionCommand command = CommandFactory.twoLegCommand(
                operationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_00L,
                LedgerTransactionType.FUNDING
        );

        PostLedgerTransactionResult result =
                new PostLedgerTransactionResult(
                        transactionId,
                        false
                );

        LedgerTransactionResponse response =
                new LedgerTransactionResponse(
                        transactionId,
                        false
                );

        when(mapper.toCommand(
                any(PostLedgerTransactionRequest.class),
                eq(operationId)
        )).thenReturn(command);

        when(postingService.post(command))
                .thenReturn(result);

        when(mapper.toResponse(result))
                .thenReturn(response);

        mockMvc.perform(
                        post(TRANSACTIONS_URL)
                                .header(
                                        IDEMPOTENCY_KEY,
                                        operationId
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestJson())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactionId")
                                .value(transactionId.toString())
                )
                .andExpect(
                        jsonPath("$.replayed")
                                .value(false)
                );

        verify(mapper).toCommand(
                any(PostLedgerTransactionRequest.class),
                eq(operationId)
        );

        verify(postingService).post(command);

        verify(mapper).toResponse(result);
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing()
            throws Exception {

        mockMvc.perform(
                        post(TRANSACTIONS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestJson())
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postingService);
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest()
            throws Exception {

        String request = """
                {
                  "transactionType": "TRANSFER",
                  "currency": "BYN",
                  "postings": [
                    {
                      "accountId": "%s",
                      "side": "DEBIT",
                      "amountMinor": 0
                    }
                  ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post(TRANSACTIONS_URL)
                                .header(
                                        IDEMPOTENCY_KEY,
                                        UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postingService);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldReturnConflictForIdempotencyConflict()
            throws Exception {

        UUID operationId = UUID.randomUUID();

        PostLedgerTransactionCommand command = CommandFactory.twoLegCommand(
                operationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_00L,
                LedgerTransactionType.FUNDING
        );

        when(mapper.toCommand(
                any(PostLedgerTransactionRequest.class),
                eq(operationId)
        )).thenReturn(command);

        when(postingService.post(command))
                .thenThrow(
                        new IdempotencyConflictException(
                                operationId
                        )
                );

        mockMvc.perform(
                        post(TRANSACTIONS_URL)
                                .header(
                                        IDEMPOTENCY_KEY,
                                        operationId
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestJson())
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReverseLedgerTransaction()
            throws Exception {

        UUID originalTransactionId =
                UUID.randomUUID();

        UUID operationId =
                UUID.randomUUID();

        UUID reversalTransactionId =
                UUID.randomUUID();

        PostLedgerTransactionResult result =
                new PostLedgerTransactionResult(
                        reversalTransactionId,
                        false
                );

        LedgerTransactionResponse response =
                new LedgerTransactionResponse(
                        reversalTransactionId,
                        false
                );

        when(reversalService.reverse(
                any(ReverseLedgerTransactionCommand.class)
        )).thenReturn(result);

        when(mapper.toResponse(result))
                .thenReturn(response);

        mockMvc.perform(
                        post(
                                TRANSACTIONS_URL
                                        + "/{transactionId}/reversal",
                                originalTransactionId
                        )
                                .header(
                                        IDEMPOTENCY_KEY,
                                        operationId
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transactionId")
                                .value(
                                        reversalTransactionId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.replayed")
                                .value(false)
                );

        verify(reversalService).reverse(
                new ReverseLedgerTransactionCommand(
                        operationId,
                        originalTransactionId
                )
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownTransaction()
            throws Exception {

        UUID transactionId =
                UUID.randomUUID();

        UUID operationId =
                UUID.randomUUID();

        when(reversalService.reverse(
                any(ReverseLedgerTransactionCommand.class)
        )).thenThrow(
                new LedgerTransactionNotFoundException(
                        transactionId
                )
        );

        mockMvc.perform(
                        post(
                                TRANSACTIONS_URL
                                        + "/{transactionId}/reversal",
                                transactionId
                        )
                                .header(
                                        IDEMPOTENCY_KEY,
                                        operationId
                                )
                )
                .andExpect(status().isNotFound());
    }


    private String validRequestJson() {
        return """
                {
                  "transactionType": "FUNDING",
                  "currency": "BYN",
                  "postings": [
                    {
                      "accountId": "%s",
                      "side": "DEBIT",
                      "amountMinor": 10000
                    },
                    {
                      "accountId": "%s",
                      "side": "CREDIT",
                      "amountMinor": 10000
                    }
                  ]
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}