package by.dkmplis.transfer_service.infrastructure.client.ledger;

import by.dkmplis.transfer_service.application.exception.LedgerCallUncertainException;
import by.dkmplis.transfer_service.application.exception.LedgerIntegrationException;
import by.dkmplis.transfer_service.application.exception.LedgerTransferRejectedException;
import by.dkmplis.transfer_service.application.port.LedgerTransferCommand;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestLedgerClientTest {

    private static final String BASE_URL =
            "http://ledger.test";

    private static final String TRANSACTIONS_URL =
            BASE_URL + "/api/v1/ledger/transactions";

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private MockRestServiceServer server;

    private RestLedgerClient ledgerClient;


    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        RestClient restClient = builder
                .baseUrl(BASE_URL)
                .build();

        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .build();

        ledgerClient = new RestLedgerClient(
                objectMapper,
                restClient
        );
    }


    @AfterEach
    void verifyServer() {
        server.verify();
    }


    @Test
    void shouldPostTransferToLedger() {
        UUID ledgerOperationId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        UUID ledgerTransactionId = UUID.randomUUID();

        LedgerTransferCommand command =
                new LedgerTransferCommand(
                        ledgerOperationId,
                        fromAccountId,
                        toAccountId,
                        "BYN",
                        10_000L
                );

        String expectedRequest = """
                {
                  "transactionType": "TRANSFER",
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
                fromAccountId,
                toAccountId
        );

        String response = """
                {
                  "transactionId": "%s",
                  "replayed": false
                }
                """.formatted(
                ledgerTransactionId
        );

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andExpect(method(POST))
                .andExpect(
                        header(
                                IDEMPOTENCY_KEY_HEADER,
                                ledgerOperationId.toString()
                        )
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(
                        content().json(
                                expectedRequest
                        )
                )
                .andRespond(
                        withSuccess(
                                response,
                                MediaType.APPLICATION_JSON
                        )
                );

        LedgerTransferResult result =
                ledgerClient.postTransfer(command);

        assertThat(result.ledgerTransactionId())
                .isEqualTo(ledgerTransactionId);

        assertThat(result.replayed())
                .isFalse();
    }


    @Test
    void shouldHandleLedgerIdempotentReplay() {
        UUID ledgerTransactionId =
                UUID.randomUUID();

        String response = """
                {
                  "transactionId": "%s",
                  "replayed": true
                }
                """.formatted(
                ledgerTransactionId
        );

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withSuccess(
                                response,
                                MediaType.APPLICATION_JSON
                        )
                );

        LedgerTransferResult result =
                ledgerClient.postTransfer(
                        command()
                );

        assertThat(result.ledgerTransactionId())
                .isEqualTo(ledgerTransactionId);

        assertThat(result.replayed())
                .isTrue();
    }


    @Test
    void shouldMapInsufficientFundsToBusinessRejection() {
        String response = """
                {
                  "status": 409,
                  "code": "INSUFFICIENT_FUNDS",
                  "message": "Insufficient funds",
                  "timestamp": "2026-09-05T10:00:00Z"
                }
                """;

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body(response)
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerTransferRejectedException.class
                )
                .hasMessage(
                        "Insufficient funds"
                );
    }


    @Test
    void shouldMapLedgerIdempotencyConflictToIntegrationError() {
        String response = """
                {
                  "status": 409,
                  "code": "IDEMPOTENCY_CONFLICT",
                  "message": "Idempotency conflict",
                  "timestamp": "2026-09-05T10:00:00Z"
                }
                """;

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body(response)
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerIntegrationException.class
                )
                .hasMessageContaining(
                        "Ledger idempotency conflict"
                );
    }


    @Test
    void shouldMapUnexpectedLedger4xxToIntegrationError() {
        String response = """
                {
                  "status": 400,
                  "code": "INVALID_REQUEST",
                  "message": "Invalid ledger request",
                  "timestamp": "2026-09-05T10:00:00Z"
                }
                """;

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body(response)
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerIntegrationException.class
                )
                .hasMessageContaining(
                        "Unexpected ledger error"
                );
    }


    @Test
    void shouldTreatLedger5xxAsUncertain() {
        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withStatus(
                                HttpStatus.SERVICE_UNAVAILABLE
                        )
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerCallUncertainException.class
                );
    }


    @Test
    void shouldTreatEmptyLedgerResponseAsUncertain() {
        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withSuccess()
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerCallUncertainException.class
                )
                .hasMessage(
                        "Ledger returned empty response"
                );
    }


    @Test
    void shouldRejectResponseWithoutTransactionId() {
        String response = """
                {
                  "transactionId": null,
                  "replayed": false
                }
                """;

        server.expect(
                        requestTo(TRANSACTIONS_URL)
                )
                .andRespond(
                        withSuccess(
                                response,
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThatThrownBy(
                () -> ledgerClient.postTransfer(
                        command()
                )
        )
                .isInstanceOf(
                        LedgerIntegrationException.class
                )
                .hasMessage(
                        "Ledger response does not contain transactionId"
                );
    }


    private LedgerTransferCommand command() {
        return new LedgerTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BYN",
                10_000L
        );
    }
}