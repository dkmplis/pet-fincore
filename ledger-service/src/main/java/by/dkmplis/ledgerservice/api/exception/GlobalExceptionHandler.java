package by.dkmplis.ledgerservice.api.exception;

import by.dkmplis.ledgerservice.api.dto.ApiError;
import by.dkmplis.ledgerservice.api.dto.LedgerErrorCode;
import by.dkmplis.ledgerservice.application.exception.IdempotencyConflictException;
import by.dkmplis.ledgerservice.application.exception.InsufficientFundsException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionAlreadyReversedException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LedgerTransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            LedgerTransactionNotFoundException exception
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                LedgerErrorCode.LEDGER_TRANSACTION_NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(LedgerTransactionAlreadyReversedException.class)
    public ResponseEntity<ApiError> handleAlreadyReversed(
            LedgerTransactionAlreadyReversedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.LEDGER_TRANSACTION_ALREADY_REVERSED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(
            IdempotencyConflictException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.IDEMPOTENCY_CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(
            InsufficientFundsException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.INSUFFICIENT_FUNDS,
                exception.getMessage()
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        "%s: %s".formatted(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                )
                .orElse("Request validation failed");

        return invalidRequest(message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(
            MissingRequestHeaderException exception
    ) {
        return invalidRequest(
                "Required request header '%s' is missing"
                        .formatted(exception.getHeaderName())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return invalidRequest(
                "Invalid value for '%s'"
                        .formatted(exception.getName())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception
    ) {
        return invalidRequest(
                "Request body is malformed"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return invalidRequest(
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                LedgerErrorCode.INTERNAL_ERROR,
                "Unexpected internal error"
        );
    }


    private ResponseEntity<ApiError> error(
            HttpStatus status,
            LedgerErrorCode code,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(new ApiError(
                        status.value(),
                        code.name(),
                        message,
                        Instant.now()
                ));
    }

    private ResponseEntity<ApiError> invalidRequest(
            String message
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                LedgerErrorCode.INVALID_REQUEST,
                message
        );
    }
}
