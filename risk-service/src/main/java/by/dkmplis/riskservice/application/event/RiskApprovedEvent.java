package by.dkmplis.riskservice.application.event;

import java.time.Instant;
import java.util.UUID;

public record RiskApprovedEvent(
        UUID eventId,
        UUID aggregateId,
        Instant occurredAt,
        RiskDecisionPayload payload
) implements IntegrationEvent<RiskDecisionPayload> {

    private static final String EVENT_TYPE =
            "risk.approved";

    private static final int EVENT_VERSION = 1;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public int eventVersion() {
        return EVENT_VERSION;
    }
}
