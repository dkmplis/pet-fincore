package by.dkmplis.ledgerservice.api.exception;

import by.dkmplis.ledgerservice.api.dto.ApiError;
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
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        HttpStatus.NOT_FOUND.value(),
                        "LEDGER_TRANSACTION_NOT_FOUND",
                        exception.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler({
            IdempotencyConflictException.class,
            InsufficientFundsException.class,
            LedgerTransactionAlreadyReversedException.class
    })
    public ResponseEntity<ApiError> handleConflict(
            IllegalStateException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        HttpStatus.CONFLICT.value(),
                        exception.getClass().getSimpleName(),
                        exception.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REQUEST",
                        exception.getMessage(),
                        Instant.now()
                ));
    }
}
