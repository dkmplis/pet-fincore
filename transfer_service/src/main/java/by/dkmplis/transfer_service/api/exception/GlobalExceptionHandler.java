package by.dkmplis.transfer_service.api.exception;

import by.dkmplis.transfer_service.api.dto.ApiError;
import by.dkmplis.transfer_service.api.dto.TransferErrorCode;
import by.dkmplis.transfer_service.application.exception.TransferIdempotencyConflictException;
import by.dkmplis.transfer_service.application.exception.TransferNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            TransferNotFoundException exception
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                TransferErrorCode.TRANSFER_NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(TransferIdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(
            TransferIdempotencyConflictException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                TransferErrorCode.IDEMPOTENCY_CONFLICT,
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
                        .formatted(
                                exception.getHeaderName()
                        )
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
        log.error(
                "Unexpected request processing error",
                exception
        );

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                TransferErrorCode.INTERNAL_ERROR,
                "Unexpected internal error"
        );
    }

    private ResponseEntity<ApiError> invalidRequest(
            String message
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                TransferErrorCode.INVALID_REQUEST,
                message
        );
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            TransferErrorCode code,
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