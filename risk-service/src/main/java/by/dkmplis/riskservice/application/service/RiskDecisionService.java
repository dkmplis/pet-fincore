package by.dkmplis.riskservice.application.service;

import by.dkmplis.riskservice.application.event.IntegrationEvent;
import by.dkmplis.riskservice.application.event.RiskApprovedEvent;
import by.dkmplis.riskservice.application.event.RiskDecisionPayload;
import by.dkmplis.riskservice.application.event.RiskRejectedEvent;
import by.dkmplis.riskservice.application.policy.RiskPolicyEngine;
import by.dkmplis.riskservice.application.port.IntegrationEventPublisher;
import by.dkmplis.riskservice.domain.enums.RiskStatus;
import by.dkmplis.riskservice.domain.model.RiskAssessment;
import by.dkmplis.riskservice.domain.policy.RiskDecision;
import by.dkmplis.riskservice.infrastructure.persistence.RiskAssessmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskDecisionService {

    private final RiskAssessmentRepository assessmentRepository;
    private final RiskPolicyEngine policyEngine;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Transactional
    public RiskStatus decide(
            UUID assessmentId
    ) {
        RiskAssessment assessment =
                assessmentRepository.findByIdForUpdate(
                                assessmentId
                        )
                        .orElseThrow(
                                () -> new EntityNotFoundException(
                                        "Risk assessment not found: %s"
                                                .formatted(assessmentId)
                                )
                        );

        if (assessment.getStatus()
                != RiskStatus.PENDING) {
            return assessment.getStatus();
        }

        RiskDecision decision =
                policyEngine.evaluate(
                        assessment
                );

        Instant now =
                Instant.now();

        switch (decision.status()) {
            case APPROVED ->
                    assessment.approve(
                            decision.reason(),
                            now
                    );

            case REJECTED ->
                    assessment.reject(
                            decision.reason(),
                            now
                    );

            case PENDING ->
                    throw new IllegalStateException(
                            "Risk policy cannot return PENDING"
                    );
        }

        RiskDecisionPayload payload = new RiskDecisionPayload(
                assessment.getId(),
                decision.reason()
        );

        IntegrationEvent<RiskDecisionPayload> event =
                switch (decision.status()) {
                    case APPROVED ->
                            new RiskApprovedEvent(
                                    UUID.randomUUID(),
                                    assessment.getTransferId(),
                                    now,
                                    payload
                            );

                    case REJECTED ->
                            new RiskRejectedEvent(
                                    UUID.randomUUID(),
                                    assessment.getTransferId(),
                                    now,
                                    payload
                            );

                    case PENDING -> throw new IllegalStateException(
                            "Risk policy cannot return PENDING"
                    );
                };

        integrationEventPublisher.publish(event);

        return assessment.getStatus();
    }
}
