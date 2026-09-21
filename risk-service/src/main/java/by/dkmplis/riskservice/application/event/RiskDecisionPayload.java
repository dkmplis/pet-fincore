package by.dkmplis.riskservice.application.event;

import java.util.UUID;

public record RiskDecisionPayload(
        UUID riskAssessmentId,
        String reason
) {
}
