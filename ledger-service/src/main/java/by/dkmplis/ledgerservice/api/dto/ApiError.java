package by.dkmplis.ledgerservice.api.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String code,
        String message,
        Instant timestamp
) {

}
