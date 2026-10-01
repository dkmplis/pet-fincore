package by.dkmplis.transfer_service.infrastructure.kafka.listener;

import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.application.service.RiskDecisionEventProcessor;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RawEventEnvelope;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RiskDecisionPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskEventsKafkaListener {

    private static final String RISK_APPROVED =
            "risk.approved";

    private static final String RISK_REJECTED =
            "risk.rejected";

    private static final int RISK_EVENT_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final RiskDecisionEventProcessor processor;

    @KafkaListener(topics = "risk.events.v1")
    public void consume(String json) {
        RawEventEnvelope raw = deserializeEnvelope(json);

        if (!isSupportedEventType(raw.eventType())) {
            log.debug(
                    "Ignoring risk event: eventType = {}",
                    raw.eventType()
            );
            return;
        }

        if (raw.eventVersion()
                != RISK_EVENT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported %s version: %d"
                            .formatted(
                                    raw.eventType(),
                                    raw.eventVersion()
                            )
            );
        }

        RiskDecisionPayload payload =
                deserializePayload(raw);

        EventEnvelope<RiskDecisionPayload> event =
                new EventEnvelope<>(
                        raw.eventId(),
                        raw.eventType(),
                        raw.eventVersion(),
                        raw.aggregateId(),
                        raw.occurredAt(),
                        payload
                );

        processor.process(
                event,
                json
        );

    }

    private RawEventEnvelope deserializeEnvelope(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    RawEventEnvelope.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize event envelope",
                    exception
            );
        }
    }

    private boolean isSupportedEventType(String eventType) {
        return RISK_APPROVED.equals(eventType)
                || RISK_REJECTED.equals(eventType);
    }

    private RiskDecisionPayload deserializePayload(RawEventEnvelope event) {
        try {
            return objectMapper.treeToValue(
                    event.payload(),
                    RiskDecisionPayload.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize risk decision payload",
                    exception
            );
        }
    }
}
