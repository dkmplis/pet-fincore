package by.dkmplis.transfer_service.api.controller;

import by.dkmplis.transfer_service.api.dto.CreateTransferRequest;
import by.dkmplis.transfer_service.application.command.TransferDetailsResponse;
import by.dkmplis.transfer_service.api.dto.TransferDetailsResult;
import by.dkmplis.transfer_service.api.dto.TransferResponse;
import by.dkmplis.transfer_service.api.exception.GlobalExceptionHandler;
import by.dkmplis.transfer_service.api.mapper.TransferApiMapper;
import by.dkmplis.transfer_service.application.command.CreateTransferCommand;
import by.dkmplis.transfer_service.application.command.CreateTransferResult;
import by.dkmplis.transfer_service.application.exception.TransferIdempotencyConflictException;
import by.dkmplis.transfer_service.application.exception.TransferNotFoundException;
import by.dkmplis.transfer_service.application.service.CreateTransferUseCase;
import by.dkmplis.transfer_service.application.service.TransferService;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
public class TransferControllerTest {

    private static final String TRANSFERS_URL =
            "/api/v1/transfers";

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private static final String CURRENCY = "BYN";

    private static final long AMOUNT_MINOR = 10_000L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTransferUseCase createTransferUseCase;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private TransferApiMapper transferApiMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTransfer() throws Exception {
        UUID externalOperationId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferRequest request =
                new CreateTransferRequest(
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        CreateTransferResult result =
                new CreateTransferResult(
                        transferId,
                        TransferState.COMPLETED,
                        false
                );

        TransferResponse response =
                new TransferResponse(
                        transferId,
                        TransferState.COMPLETED,
                        false
                );

        when(transferApiMapper.toCommand(
                any(CreateTransferRequest.class),
                eq(externalOperationId)
        )).thenReturn(command);

        when(createTransferUseCase.execute(command))
                .thenReturn(result);

        when(transferApiMapper.toResponse(result))
                .thenReturn(response);

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        externalOperationId
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                TRANSFERS_URL + "/" + transferId
                        )
                )
                .andExpect(
                        jsonPath("$.state")
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath("$.replayed")
                                .value(false)
                );

        verify(createTransferUseCase)
                .execute(command);
    }


    @Test
    void shouldRejectNonPositiveAmount()
            throws Exception {

        CreateTransferRequest request =
                createRequest(0);

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(createTransferUseCase);
    }


    @Test
    void shouldRejectInvalidCurrency()
            throws Exception {

        CreateTransferRequest request =
                new CreateTransferRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "byn",
                        AMOUNT_MINOR
                );

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(createTransferUseCase);
    }


    @Test
    void shouldReturnConflictForDifferentRequestWithSameIdempotencyKey()
            throws Exception {

        UUID externalOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferRequest request =
                new CreateTransferRequest(
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        when(transferApiMapper.toCommand(
                any(CreateTransferRequest.class),
                eq(externalOperationId)
        )).thenReturn(command);

        when(createTransferUseCase.execute(command))
                .thenThrow(
                        new TransferIdempotencyConflictException(
                                externalOperationId
                        )
                );

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        externalOperationId
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("IDEMPOTENCY_CONFLICT")
                );
    }


    @Test
    void shouldReturnTransfer() throws Exception {
        UUID transferId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        TransferDetailsResult result =
                new TransferDetailsResult(
                        transferId,
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR,
                        TransferState.PENDING
                );

        TransferDetailsResponse response =
                new TransferDetailsResponse(
                        transferId,
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR,
                        TransferState.PENDING
                );

        when(transferService.get(transferId))
                .thenReturn(result);

        when(transferApiMapper.toResponse(result))
                .thenReturn(response);

        mockMvc.perform(
                        get(
                                TRANSFERS_URL + "/{transferId}",
                                transferId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transferId")
                                .value(transferId.toString())
                )
                .andExpect(
                        jsonPath("$.fromAccountId")
                                .value(fromAccountId.toString())
                )
                .andExpect(
                        jsonPath("$.toAccountId")
                                .value(toAccountId.toString())
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value(CURRENCY)
                )
                .andExpect(
                        jsonPath("$.amountMinor")
                                .value(AMOUNT_MINOR)
                )
                .andExpect(
                        jsonPath("$.state")
                                .value("PENDING")
                );
    }


    @Test
    void shouldReturnNotFoundForUnknownTransfer()
            throws Exception {

        UUID transferId = UUID.randomUUID();

        when(transferService.get(transferId))
                .thenThrow(
                        new TransferNotFoundException(
                                transferId
                        )
                );

        mockMvc.perform(
                        get(
                                TRANSFERS_URL + "/{transferId}",
                                transferId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("TRANSFER_NOT_FOUND")
                );
    }

    @Test
    void shouldRejectRequestWithoutIdempotencyKey()
            throws Exception {

        CreateTransferRequest request =
                createRequest(AMOUNT_MINOR);

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Required request header 'Idempotency-Key' is missing"
                                )
                );

        verifyNoInteractions(createTransferUseCase);
    }

    @Test
    void shouldRejectInvalidIdempotencyKey()
            throws Exception {

        CreateTransferRequest request =
                createRequest(AMOUNT_MINOR);

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        "not-a-uuid"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(createTransferUseCase);
    }

    @Test
    void shouldRejectMalformedRequestBody()
            throws Exception {

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        UUID.randomUUID()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fromAccountId":
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Request body is malformed")
                );

        verifyNoInteractions(createTransferUseCase);
    }

    @Test
    void shouldRejectInvalidTransferId()
            throws Exception {

        mockMvc.perform(
                        get(
                                TRANSFERS_URL + "/{transferId}",
                                "not-a-uuid"
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(transferService);
    }

    @Test
    void shouldReturnOkForIdempotentReplay()
            throws Exception {

        UUID externalOperationId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreateTransferRequest request =
                new CreateTransferRequest(
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        CreateTransferCommand command =
                new CreateTransferCommand(
                        externalOperationId,
                        fromAccountId,
                        toAccountId,
                        CURRENCY,
                        AMOUNT_MINOR
                );

        CreateTransferResult result =
                new CreateTransferResult(
                        transferId,
                        TransferState.COMPLETED,
                        true
                );

        TransferResponse response =
                new TransferResponse(
                        transferId,
                        TransferState.COMPLETED,
                        true
                );

        when(transferApiMapper.toCommand(
                any(CreateTransferRequest.class),
                eq(externalOperationId)
        )).thenReturn(command);

        when(createTransferUseCase.execute(command))
                .thenReturn(result);

        when(transferApiMapper.toResponse(result))
                .thenReturn(response);

        mockMvc.perform(
                        post(TRANSFERS_URL)
                                .header(
                                        IDEMPOTENCY_KEY_HEADER,
                                        externalOperationId
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.transferId")
                                .value(transferId.toString())
                )
                .andExpect(
                        jsonPath("$.state")
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath("$.replayed")
                                .value(true)
                );
    }


    private CreateTransferRequest createRequest(
            long amountMinor
    ) {
        return new CreateTransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CURRENCY,
                amountMinor
        );
    }
}
