package by.dkmplis.riskservice.application.service;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.repository.RiskAssessmentRepository;
import by.dkmplis.riskservice.infrastructure.inbox.persistence.InboxEventRepository;
import by.dkmplis.riskservice.kafka.dto.EventEnvelope;
import by.dkmplis.riskservice.kafka.dto.TransferCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferCreatedEventProcessor {
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public void process(
            EventEnvelope<TransferCreatedPayload> event,
            String rawPayload
    ) {
        int registered = inboxEventRepository.register(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateId(),
                rawPayload
        );

        if (registered == 0) {
            return;
        }

        TransferCreatedPayload payload = event.payload();

        RiskAssessment assessment = new RiskAssessment(
                UUID.randomUUID(),
                event.aggregateId(),
                payload.fromAccountId(),
                payload.toAccountId(),
                payload.currency(),
                payload.amountMinor()
        );

        riskAssessmentRepository.save(assessment);

        int processed = inboxEventRepository.markProcessed(
                event.eventId(),
                Instant.now()
        );

        if (processed != 1) {
            throw new IllegalStateException(
                    "Failed to mark inbox event as processed"
            );
        }
    }

}
