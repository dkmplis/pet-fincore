package by.dkmplis.transfer_service.infrastructure.client.ledger;

import by.dkmplis.transfer_service.application.exception.LedgerCallUncertainException;
import by.dkmplis.transfer_service.application.exception.LedgerIntegrationException;
import by.dkmplis.transfer_service.application.exception.LedgerTransferRejectedException;
import by.dkmplis.transfer_service.application.port.LedgerClient;
import by.dkmplis.transfer_service.application.port.LedgerTransferCommand;
import by.dkmplis.transfer_service.application.port.LedgerTransferResult;
import by.dkmplis.transfer_service.infrastructure.client.ledger.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RestLedgerClient implements LedgerClient {

    private static final String TRANSACTIONS_PATH =
            "/api/v1/ledger/transactions";

    private static final String IDEMPOTENCY_KEY_HEADER =
            "Idempotency-Key";

    private static final String INSUFFICIENT_FUNDS =
            "INSUFFICIENT_FUNDS";

    private static final String IDEMPOTENCY_CONFLICT =
            "IDEMPOTENCY_CONFLICT";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Override
    public LedgerTransferResult postTransfer(
            LedgerTransferCommand command
    ) {
        LedgerTransactionRequest request = buildRequest(command);

        try {
            LedgerTransactionResponse response = restClient.post()
                    .uri(TRANSACTIONS_PATH)
                    .header(
                            IDEMPOTENCY_KEY_HEADER,
                            command.ledgerOperationId().toString()
                    )
                    .body(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (httpRequest, httpResponse) ->
                                    handle4xx(httpResponse)
                    )
                    .body(LedgerTransactionResponse.class);

            if (response == null) {
                throw new LedgerCallUncertainException(
                        "Ledger returned empty response"
                );
            }

            if (response.transactionId() == null) {
                throw new LedgerIntegrationException(
                        "Ledger response does not contain transactionId"
                );
            }

            return new LedgerTransferResult(
                    response.transactionId(),
                    response.replayed()
            );
        } catch (ResourceAccessException exception) {
            throw new LedgerCallUncertainException(
                    "Ledger call result is uncertain",
                    exception
            );
        } catch (RestClientException exception) {
            throw new LedgerCallUncertainException(
                    "Unexpected error while calling ledger",
                    exception
            );
        }
    }

    private LedgerTransactionRequest buildRequest(
            LedgerTransferCommand command
    ) {
        return new LedgerTransactionRequest(
                LedgerTransactionType.TRANSFER,
                command.currency(),
                List.of(
                        new LedgerTransactionRequest.PostingRequest(
                                command.fromAccountId(),
                                PostingSide.DEBIT,
                                command.amountMinor()
                        ),
                        new LedgerTransactionRequest.PostingRequest(
                                command.toAccountId(),
                                PostingSide.CREDIT,
                                command.amountMinor()
                        )
                )
        );
    }

    private void handle4xx(
            ClientHttpResponse response
    ) throws IOException {

        LedgerApiError error = objectMapper.readValue(
                response.getBody(),
                LedgerApiError.class
        );

        switch (error.code()) {

            case INSUFFICIENT_FUNDS ->
                    throw new LedgerTransferRejectedException(
                            error.message()
                    );

            case IDEMPOTENCY_CONFLICT ->
                    throw new LedgerIntegrationException(
                            "Ledger idempotency conflict: "
                                    + error.message()
                    );

            default ->
                    throw new LedgerIntegrationException(
                            """
                            Unexpected ledger error: \
                            status=%d, code=%s, message=%s
                            """.formatted(
                                    error.status(),
                                    error.code(),
                                    error.message()
                            )
                    );
        }
    }

}
