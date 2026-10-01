package by.dkmplis.transfer_service.infrastructure.kafka.dto;

import java.util.UUID;

public record RiskDecisionPayload(
        UUID riskAssessmentId,
        String reason
) {
}
