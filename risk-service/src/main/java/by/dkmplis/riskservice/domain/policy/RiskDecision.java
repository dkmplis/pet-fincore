package by.dkmplis.riskservice.domain.policy;

import by.dkmplis.riskservice.domain.enums.RiskStatus;

public record RiskDecision(
        RiskStatus status,
        String reason
) {
    public RiskDecision {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Risk status is required"
            );
        }

        if (status == RiskStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Risk decision cannot be PENDING"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Risk decision reason is required"
            );
        }
    }
}
