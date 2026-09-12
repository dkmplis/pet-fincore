package by.dkmplis.riskservice.kafka.dto.listener;

import by.dkmplis.riskservice.application.service.TransferCreatedEventProcessor;
import by.dkmplis.riskservice.kafka.dto.EventEnvelope;
import by.dkmplis.riskservice.kafka.dto.RawEventEnvelope;
import by.dkmplis.riskservice.kafka.dto.TransferCreatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventsKafkaListener {

    private static final String TRANSFER_CREATED =
            "transfer.created";

    private static final int TRANSFER_CREATED_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final TransferCreatedEventProcessor createdEventProcessor;

    @KafkaListener(topics = "transfer.events.v1")
    public void consume(String json) {
        RawEventEnvelope raw = deserializeEnvelope(json);

        if (!TRANSFER_CREATED.equals(raw.eventType())) {
            log.debug(
                    "Ignoring transfer event: eventType={}",
                    raw.eventType()
            );
            return;
        }

        if (raw.eventVersion() != TRANSFER_CREATED_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported transfer.created version: %d"
                            .formatted(raw.eventVersion())
            );
        }

        TransferCreatedPayload payload = deserializePayload(raw);

        EventEnvelope<TransferCreatedPayload> event = new EventEnvelope<>(
                raw.eventId(),
                raw.eventType(),
                raw.eventVersion(),
                raw.aggregateId(),
                raw.occurredAt(),
                payload
        );

        createdEventProcessor.process(
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

    private TransferCreatedPayload deserializePayload(
            RawEventEnvelope event
    ) {
        try {
            return objectMapper.treeToValue(
                    event.payload(),
                    TransferCreatedPayload.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize transfer.created payload",
                    exception
            );
        }
    }

}
