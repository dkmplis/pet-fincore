package by.dkmplis.ledgerservice.api.exception;

import by.dkmplis.ledgerservice.api.dto.ApiError;
import by.dkmplis.ledgerservice.api.dto.LedgerErrorCode;
import by.dkmplis.ledgerservice.application.exception.IdempotencyConflictException;
import by.dkmplis.ledgerservice.application.exception.InsufficientFundsException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionAlreadyReversedException;
import by.dkmplis.ledgerservice.application.exception.LedgerTransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public ResponseEntity<ApiError> handleConflict(
            LedgerTransactionAlreadyReversedException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.LEDGER_TRANSACTION_ALREADY_REVERSED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            IdempotencyConflictException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.IDEMPOTENCY_CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleConflict(
            InsufficientFundsException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                LedgerErrorCode.INSUFFICIENT_FUNDS,
                exception.getMessage()
        );
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                LedgerErrorCode.INVALID_REQUEST,
                exception.getMessage()
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
}
