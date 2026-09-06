package by.dkmplis.transfer_service.api.dto;

public enum TransferErrorCode {
    TRANSFER_NOT_FOUND,
    IDEMPOTENCY_CONFLICT,
    INVALID_REQUEST,
    INTERNAL_ERROR
}
