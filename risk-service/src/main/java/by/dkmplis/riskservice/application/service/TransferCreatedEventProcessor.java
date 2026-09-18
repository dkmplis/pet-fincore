package by.dkmplis.riskservice.application.service;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskDecision;
import by.dkmplis.riskservice.infrastructure.persistence.RiskAssessmentRepository;
import by.dkmplis.riskservice.infrastructure.inbox.persistence.InboxEventRepository;
import by.dkmplis.riskservice.infrastructure.kafka.dto.EventEnvelope;
import by.dkmplis.riskservice.infrastructure.kafka.dto.TransferCreatedPayload;
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
    private final RiskDecisionService riskDecisionService;

    @Transactional
    public void process(
            EventEnvelope<TransferCreatedPayload> event,
            String rawEventJson
    ) {
        int registered = inboxEventRepository.register(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateId(),
                rawEventJson
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

        riskAssessmentRepository.saveAndFlush(assessment);

        riskDecisionService.decide(assessment.getId());

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
