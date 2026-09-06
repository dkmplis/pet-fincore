package by.dkmplis.transfer_service.api.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String code,
        String message,
        Instant timestamp
) {
}
